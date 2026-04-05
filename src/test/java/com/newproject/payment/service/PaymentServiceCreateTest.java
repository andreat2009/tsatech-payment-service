package com.newproject.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newproject.payment.domain.Payment;
import com.newproject.payment.domain.PaymentMethod;
import com.newproject.payment.domain.PaymentTransaction;
import com.newproject.payment.dto.PaymentRequest;
import com.newproject.payment.dto.PaymentResponse;
import com.newproject.payment.events.EventPublisher;
import com.newproject.payment.repository.PaymentMethodRepository;
import com.newproject.payment.repository.PaymentRepository;
import com.newproject.payment.repository.PaymentTransactionRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceCreateTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private PayPalClient payPalClient;
    @Mock
    private FabrickClient fabrickClient;
    @Mock
    private PaymentMethodProviderConfigurationResolver providerConfigurationResolver;
    @Mock
    private PaymentCredentialCryptoService paymentCredentialCryptoService;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
            paymentRepository,
            paymentMethodRepository,
            paymentTransactionRepository,
            eventPublisher,
            payPalClient,
            fabrickClient,
            providerConfigurationResolver,
            paymentCredentialCryptoService,
            new ObjectMapper()
        );

        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(eventPublisher).publish(any(), any(), any(), any());
    }

    @Test
    void createOfflinePaymentSetsInitialStatusBeforeFirstSave() {
        PaymentMethod method = offlineMethod();
        when(paymentMethodRepository.findByCode("bank_transfer")).thenReturn(Optional.of(method));
        when(paymentRepository.findFirstByOrderIdOrderByIdAsc(42L)).thenReturn(Optional.empty());
        List<String> persistedStatuses = new ArrayList<>();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            persistedStatuses.add(payment.getStatus());
            if (payment.getId() == null) {
                payment.setId(100L);
            }
            return payment;
        });

        PaymentRequest request = new PaymentRequest();
        request.setOrderId(42L);
        request.setAmount(new BigDecimal("99.90"));
        request.setCurrency("EUR");
        request.setMethodCode("bank_transfer");
        request.setProvider("bank_transfer");
        request.setStatus("CREATED");

        PaymentResponse response = paymentService.create(request);

        assertThat(persistedStatuses).isNotEmpty();
        assertThat(persistedStatuses.get(0)).isEqualTo("CREATED");
        assertThat(response.getStatus()).isEqualTo("CREATED");
        assertThat(response.getMethodCode()).isEqualTo("bank_transfer");
        verify(payPalClient, never()).createOrder(any(), any(), any());
    }

    @Test
    void createPayPalPaymentPersistsInitialStatusThenTransitionsToRedirectRequired() {
        PaymentMethod method = new PaymentMethod();
        method.setCode("paypal");
        method.setDisplayName("PayPal");
        method.setProvider("PAYPAL");
        method.setPaymentFlow("REDIRECT");
        method.setActive(true);
        method.setSortOrder(30);

        PaymentMethodProviderConfigurationResolver.ResolvedPayPalConfig config =
            new PaymentMethodProviderConfigurationResolver.ResolvedPayPalConfig(
                "production",
                "client-id",
                "client-secret",
                "https://api-m.paypal.com",
                "TSATech",
                null,
                "stored"
            );

        when(paymentMethodRepository.findByCode("paypal")).thenReturn(Optional.of(method));
        when(paymentRepository.findFirstByOrderIdOrderByIdAsc(77L)).thenReturn(Optional.empty());
        List<String> persistedStatuses = new ArrayList<>();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            persistedStatuses.add(payment.getStatus());
            if (payment.getId() == null) {
                payment.setId(200L);
            }
            return payment;
        });
        when(providerConfigurationResolver.resolvePayPal(method)).thenReturn(config);
        when(payPalClient.isAvailable(config)).thenReturn(true);
        when(payPalClient.createOrder(any(), any(), any())).thenReturn(
            new PayPalClient.PayPalCreateResult("PP-ORDER-1", "https://paypal.example/approve", "CREATED")
        );

        PaymentRequest request = new PaymentRequest();
        request.setOrderId(77L);
        request.setAmount(new BigDecimal("19.90"));
        request.setCurrency("EUR");
        request.setMethodCode("paypal");
        request.setProvider("paypal");
        request.setStatus("CREATED");
        request.setReturnUrl("https://store.example/checkout/payment/paypal/complete?orderId=77");
        request.setCancelUrl("https://store.example/checkout/payment/paypal/cancel?orderId=77");

        PaymentResponse response = paymentService.create(request);

        assertThat(persistedStatuses).isNotEmpty();
        assertThat(persistedStatuses.get(0)).isEqualTo("CREATED");
        assertThat(response.getStatus()).isEqualTo("REDIRECT_REQUIRED");
        assertThat(response.getProviderOrderId()).isEqualTo("PP-ORDER-1");
        assertThat(response.getRedirectUrl()).isEqualTo("https://paypal.example/approve");
    }

    private PaymentMethod offlineMethod() {
        PaymentMethod method = new PaymentMethod();
        method.setCode("bank_transfer");
        method.setDisplayName("Bank transfer");
        method.setProvider("OFFLINE");
        method.setPaymentFlow("OFFLINE");
        method.setActive(true);
        method.setSortOrder(20);
        return method;
    }
}
