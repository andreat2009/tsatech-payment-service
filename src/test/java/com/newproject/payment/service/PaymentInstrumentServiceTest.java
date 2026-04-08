package com.newproject.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newproject.payment.domain.PaymentInstrument;
import com.newproject.payment.domain.PaymentMethod;
import com.newproject.payment.dto.PaymentInstrumentRequest;
import com.newproject.payment.repository.PaymentInstrumentRepository;
import com.newproject.payment.repository.PaymentMethodRepository;
import com.newproject.payment.security.RequestActor;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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
    @Mock
    private PayPalClient payPalClient;

    @InjectMocks
    private PaymentInstrumentService paymentInstrumentService;

    @Test
    void createExchangesPayPalSetupTokenBeforePersisting() {
        PaymentMethod method = new PaymentMethod();
        method.setCode("paypal");
        method.setProvider("PAYPAL");
        method.setPaymentFlow("REDIRECT");
        method.setDisplayName("PayPal");
        method.setActive(true);

        PaymentInstrumentRequest request = new PaymentInstrumentRequest();
        request.setPaymentMethodCode("paypal");
        request.setProviderToken("vault-setup-token");
        request.setProviderTokenType("PAYPAL_SETUP_TOKEN");
        request.setDefaultInstrument(true);
        request.setActive(true);

        when(requestActor.resolveScopedCustomerId(42L)).thenReturn(42L);
        when(paymentMethodRepository.findByCode("paypal")).thenReturn(Optional.of(method));
        when(providerConfigurationResolver.resolvePayPal(method)).thenReturn(
            new PaymentMethodProviderConfigurationResolver.ResolvedPayPalConfig(
                "production",
                "client-id",
                "client-secret",
                "https://api-m.paypal.com",
                "TSA Store",
                null,
                "stored"
            )
        );
        when(payPalClient.createPaymentTokenFromSetupToken(any(), any())).thenReturn(
            new PayPalClient.PayPalPaymentTokenResult("vault-payment-token", "pp-customer-1", "shopper@example.com", "payer-1")
        );
        when(paymentCredentialCryptoService.encrypt("vault-payment-token")).thenReturn("encrypted-token");
        when(paymentInstrumentRepository.findFirstByCustomerIdAndProviderTokenFingerprint(any(), any())).thenReturn(Optional.empty());
        when(paymentInstrumentRepository.save(any())).thenAnswer(invocation -> {
            PaymentInstrument instrument = invocation.getArgument(0);
            instrument.setId(77L);
            return instrument;
        });

        var response = paymentInstrumentService.create(42L, request);

        ArgumentCaptor<PaymentInstrument> captor = ArgumentCaptor.forClass(PaymentInstrument.class);
        verify(paymentInstrumentRepository).save(captor.capture());
        PaymentInstrument saved = captor.getValue();
        assertEquals("paypal", saved.getPaymentMethodCode());
        assertEquals("PAYPAL", saved.getProvider());
        assertEquals("pp-customer-1", saved.getGatewayCustomerReference());
        assertEquals("PayPal - shopper@example.com", saved.getDisplayLabel());
        assertEquals("PayPal", saved.getBrand());
        assertEquals(77L, response.getId());
        assertTrue(Boolean.TRUE.equals(response.getTokenStored()));
    }
}
