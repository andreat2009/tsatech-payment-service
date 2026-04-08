package com.newproject.payment.service;

import com.newproject.payment.domain.PaymentMethod;
import com.newproject.payment.dto.PayPalBrowserVaultSessionResponse;
import com.newproject.payment.dto.PayPalSetupTokenResponse;
import com.newproject.payment.exception.BadRequestException;
import com.newproject.payment.repository.PaymentMethodRepository;
import com.newproject.payment.security.RequestActor;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentVaultService {
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentMethodProviderConfigurationResolver providerConfigurationResolver;
    private final PayPalClient payPalClient;
    private final RequestActor requestActor;

    public PaymentVaultService(
        PaymentMethodRepository paymentMethodRepository,
        PaymentMethodProviderConfigurationResolver providerConfigurationResolver,
        PayPalClient payPalClient,
        RequestActor requestActor
    ) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.providerConfigurationResolver = providerConfigurationResolver;
        this.payPalClient = payPalClient;
        this.requestActor = requestActor;
    }

    @Transactional(readOnly = true)
    public PayPalBrowserVaultSessionResponse createPayPalBrowserVaultSession(Long customerId, String paymentMethodCode) {
        requestActor.resolveScopedCustomerId(customerId);
        PaymentMethod method = resolvePayPalMethod(paymentMethodCode);
        PaymentMethodProviderConfigurationResolver.ResolvedPayPalConfig config = providerConfigurationResolver.resolvePayPal(method);
        if (!config.isAvailable()) {
            throw new BadRequestException("PayPal credentials are not configured");
        }

        PayPalBrowserVaultSessionResponse response = new PayPalBrowserVaultSessionResponse();
        response.setPaymentMethodCode(method.getCode());
        response.setDisplayName(method.getDisplayName());
        response.setProvider("PAYPAL");
        response.setProviderBrandName(config.brandName());
        response.setClientId(config.clientId());
        response.setUserIdToken(payPalClient.obtainUserIdToken(config));
        response.setSdkUrl(payPalClient.buildJavascriptSdkUrl(config));
        return response;
    }

    @Transactional(readOnly = true)
    public PayPalSetupTokenResponse createPayPalSetupToken(Long customerId, String paymentMethodCode) {
        requestActor.resolveScopedCustomerId(customerId);
        PaymentMethod method = resolvePayPalMethod(paymentMethodCode);
        PaymentMethodProviderConfigurationResolver.ResolvedPayPalConfig config = providerConfigurationResolver.resolvePayPal(method);
        if (!config.isAvailable()) {
            throw new BadRequestException("PayPal credentials are not configured");
        }

        PayPalClient.PayPalSetupTokenResult result = payPalClient.createVaultSetupToken(config);
        PayPalSetupTokenResponse response = new PayPalSetupTokenResponse();
        response.setPaymentMethodCode(method.getCode());
        response.setSetupToken(result.setupToken());
        response.setProviderCustomerId(result.providerCustomerId());
        response.setStatus(result.status());
        return response;
    }

    private PaymentMethod resolvePayPalMethod(String paymentMethodCode) {
        String normalizedCode = paymentMethodCode == null ? null : paymentMethodCode.trim();
        if (normalizedCode == null || normalizedCode.isBlank()) {
            throw new BadRequestException("Payment method code is required");
        }
        PaymentMethod method = paymentMethodRepository.findByCode(normalizedCode)
            .filter(PaymentMethod::getActive)
            .orElseThrow(() -> new BadRequestException("Payment method not available: " + normalizedCode));
        String provider = method.getProvider() == null ? "" : method.getProvider().trim().toUpperCase(Locale.ROOT);
        if (!"PAYPAL".equals(provider)) {
            throw new BadRequestException("Browser vault session is only supported for PayPal methods");
        }
        return method;
    }
}
