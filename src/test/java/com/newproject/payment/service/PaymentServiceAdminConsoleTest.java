package com.newproject.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newproject.payment.domain.Payment;
import com.newproject.payment.domain.PaymentTransaction;
import com.newproject.payment.events.EventPublisher;
import com.newproject.payment.repository.PaymentMethodRepository;
import com.newproject.payment.repository.PaymentRepository;
import com.newproject.payment.repository.PaymentTransactionRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class PaymentServiceAdminConsoleTest {

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
    }

    @Test
    void listWithFiltersUsesSpecificationQuery() {
        Payment payment = new Payment();
        payment.setId(10L);
        payment.setOrderId(50L);
        payment.setStatus("FAILED");
        payment.setProvider("PAYPAL");

        when(paymentRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(payment));

        assertThat(paymentService.list(null, "FAILED", "PAYPAL", true, "PP-50"))
            .extracting("id")
            .containsExactly(10L);

        verify(paymentRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void listTransactionsMapsTimelineEntries() {
        Payment payment = new Payment();
        payment.setId(7L);
        payment.setOrderId(20L);
        when(paymentRepository.findById(7L)).thenReturn(Optional.of(payment));

        PaymentTransaction tx = new PaymentTransaction();
        tx.setId(99L);
        tx.setPayment(payment);
        tx.setOrderId(20L);
        tx.setOperationType("CAPTURE");
        tx.setEventSource("PAYPAL_WEBHOOK");
        tx.setStatus("CAPTURED");
        tx.setProviderStatus("COMPLETED");
        tx.setProviderReference("CAP-1");
        tx.setAmount(new BigDecimal("19.90"));
        tx.setCurrency("EUR");
        tx.setFailureCode(null);
        tx.setFailureReason(null);
        tx.setRawPayload("payload");
        tx.setCreatedAt(OffsetDateTime.parse("2026-04-08T10:05:00Z"));
        tx.setUpdatedAt(OffsetDateTime.parse("2026-04-08T10:05:00Z"));
        when(paymentTransactionRepository.findByPaymentIdOrderByIdDesc(7L)).thenReturn(List.of(tx));

        assertThat(paymentService.listTransactions(7L))
            .singleElement()
            .satisfies(response -> {
                assertThat(response.getId()).isEqualTo(99L);
                assertThat(response.getPaymentId()).isEqualTo(7L);
                assertThat(response.getProviderReference()).isEqualTo("CAP-1");
                assertThat(response.getRawPayload()).isEqualTo("payload");
            });
    }
}
