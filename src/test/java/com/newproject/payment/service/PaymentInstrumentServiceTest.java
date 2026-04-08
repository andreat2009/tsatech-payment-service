package com.newproject.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.newproject.payment.domain.PaymentInstrument;
import com.newproject.payment.domain.PaymentMethod;
import com.newproject.payment.dto.PaymentInstrumentRequest;
import com.newproject.payment.dto.PaymentInstrumentResponse;
import com.newproject.payment.repository.PaymentInstrumentRepository;
import com.newproject.payment.repository.PaymentMethodRepository;
import com.newproject.payment.security.RequestActor;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentInstrumentServiceTest {

    @Mock
    private PaymentInstrumentRepository paymentInstrumentRepository;
    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    @Mock
    private PaymentCredentialCryptoService paymentCredentialCryptoService;
    @Mock
    private PaymentMethodProviderConfigurationResolver providerConfigurationResolver;
    @Mock
    private RequestActor requestActor;

    private PaymentInstrumentService paymentInstrumentService;

    @BeforeEach
    void setUp() {
        paymentInstrumentService = new PaymentInstrumentService(
            paymentInstrumentRepository,
            paymentMethodRepository,
            paymentCredentialCryptoService,
            providerConfigurationResolver,
            requestActor
        );
    }

    @Test
    void createStoresOnlyEncryptedProviderToken() {
        PaymentMethod method = new PaymentMethod();
        method.setCode("paypal");
        method.setProvider("PAYPAL");
        method.setPaymentFlow("REDIRECT");
        method.setActive(true);

        when(requestActor.resolveScopedCustomerId(42L)).thenReturn(42L);
        when(paymentMethodRepository.findByCode("paypal")).thenReturn(Optional.of(method));
        when(providerConfigurationResolver.resolvePayPal(method)).thenReturn(
            new PaymentMethodProviderConfigurationResolver.ResolvedPayPalConfig("production", "client", "secret", "https://api-m.paypal.com", "TSA", "webhook", "stored")
        );
        when(paymentCredentialCryptoService.encrypt("tok_live_reference")).thenReturn("enc:v1:token");
        when(paymentInstrumentRepository.findFirstByCustomerIdAndProviderTokenFingerprint(any(), any())).thenReturn(Optional.empty());
        when(paymentInstrumentRepository.save(any(PaymentInstrument.class))).thenAnswer(invocation -> {
            PaymentInstrument instrument = invocation.getArgument(0);
            instrument.setId(99L);
            if (instrument.getCreatedAt() == null) {
                instrument.setCreatedAt(OffsetDateTime.now());
            }
            if (instrument.getUpdatedAt() == null) {
                instrument.setUpdatedAt(OffsetDateTime.now());
            }
            return instrument;
        });

        PaymentInstrumentRequest request = new PaymentInstrumentRequest();
        request.setPaymentMethodCode("paypal");
        request.setProviderToken("tok_live_reference");
        request.setDisplayLabel("PayPal vault");
        request.setBrand("PayPal");
        request.setLast4("4242");
        request.setDefaultInstrument(true);
        request.setActive(true);

        PaymentInstrumentResponse response = paymentInstrumentService.create(42L, request);

        assertEquals(99L, response.getId());
        assertEquals("paypal", response.getPaymentMethodCode());
        assertEquals("PAYPAL", response.getProvider());
        assertEquals("4242", response.getLast4());
        assertTrue(Boolean.TRUE.equals(response.getTokenStored()));
        assertTrue(Boolean.TRUE.equals(response.getDefaultInstrument()));
    }
}
