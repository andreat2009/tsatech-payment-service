package com.newproject.payment.service;

import com.newproject.payment.domain.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class PaymentMethodProviderConfigurationResolver {
    private final PaymentProviderProperties providerProperties;
    private final PaymentCredentialCryptoService cryptoService;

    public PaymentMethodProviderConfigurationResolver(
        PaymentProviderProperties providerProperties,
        PaymentCredentialCryptoService cryptoService
    ) {
        this.providerProperties = providerProperties;
        this.cryptoService = cryptoService;
    }

    public ResolvedPayPalConfig resolvePayPal(PaymentMethod method) {
        PaymentProviderProperties.PayPal fallback = providerProperties.getPaypal();
        String storedClientSecret = cryptoService.decrypt(method != null ? method.getProviderClientSecretEncrypted() : null);
        String effectiveClientSecret = firstNonBlank(storedClientSecret, fallback.getClientSecret());
        return new ResolvedPayPalConfig(
            firstNonBlank(method != null ? method.getProviderEnvironment() : null, fallback.getEnvironment()),
            firstNonBlank(method != null ? method.getProviderClientId() : null, fallback.getClientId()),
            effectiveClientSecret,
            firstNonBlank(method != null ? method.getProviderBaseUrl() : null, fallback.getBaseUrl()),
            firstNonBlank(method != null ? method.getProviderBrandName() : null, fallback.getBrandName()),
            firstNonBlank(method != null ? method.getProviderWebhookId() : null, fallback.getWebhookId()),
            credentialSource(hasText(method != null ? method.getProviderClientSecretEncrypted() : null), hasText(fallback.getClientSecret()))
        );
    }

    public ResolvedFabrickConfig resolveFabrick(PaymentMethod method) {
        PaymentProviderProperties.Fabrick fallback = providerProperties.getFabrick();
        String storedApiKey = cryptoService.decrypt(method != null ? method.getProviderApiKeyEncrypted() : null);
        String effectiveApiKey = firstNonBlank(storedApiKey, fallback.getApiKey());
        return new ResolvedFabrickConfig(
            firstNonBlank(method != null ? method.getProviderEnvironment() : null, fallback.getEnvironment()),
            effectiveApiKey,
            firstNonBlank(method != null ? method.getProviderShopLogin() : null, fallback.getShopLogin()),
            firstNonBlank(method != null ? method.getProviderBaseUrl() : null, fallback.getBaseUrl()),
            firstNonBlank(method != null ? method.getProviderLightboxScriptUrl() : null, fallback.getLightboxScriptUrl()),
            firstNonBlank(method != null ? method.getProviderNotificationUrl() : null, fallback.getNotificationUrl()),
            credentialSource(hasText(method != null ? method.getProviderApiKeyEncrypted() : null), hasText(fallback.getApiKey()))
        );
    }

    private String credentialSource(boolean stored, boolean fallback) {
        if (stored) {
            return "stored";
        }
        if (fallback) {
            return "fallback";
        }
        return "missing";
    }

    private String firstNonBlank(String preferred, String fallback) {
        return hasText(preferred) ? preferred.trim() : (hasText(fallback) ? fallback.trim() : null);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record ResolvedPayPalConfig(
        String environment,
        String clientId,
        String clientSecret,
        String baseUrl,
        String brandName,
        String webhookId,
        String clientSecretSource
    ) {
        public boolean isAvailable() {
            return hasText(clientId) && hasText(clientSecret) && hasText(baseUrl);
        }

        public boolean isWebhookVerificationAvailable() {
            return isAvailable() && hasText(webhookId);
        }

        private boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }

    public record ResolvedFabrickConfig(
        String environment,
        String apiKey,
        String shopLogin,
        String baseUrl,
        String lightboxScriptUrl,
        String notificationUrl,
        String apiKeySource
    ) {
        public boolean isAvailable() {
            return hasText(apiKey) && hasText(shopLogin) && hasText(baseUrl);
        }

        private boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }
}
