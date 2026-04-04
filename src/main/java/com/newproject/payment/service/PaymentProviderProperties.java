package com.newproject.payment.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "providers")
public class PaymentProviderProperties {
    private final PayPal paypal = new PayPal();
    private final Fabrick fabrick = new Fabrick();

    public PayPal getPaypal() { return paypal; }
    public Fabrick getFabrick() { return fabrick; }

    public static class PayPal {
        private boolean enabled = true;
        private String environment = "sandbox";
        private String clientId;
        private String clientSecret;
        private String baseUrl = "https://api-m.sandbox.paypal.com";
        private String brandName = "TSA Store";
        private String webhookId;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getBrandName() { return brandName; }
        public void setBrandName(String brandName) { this.brandName = brandName; }
        public String getWebhookId() { return webhookId; }
        public void setWebhookId(String webhookId) { this.webhookId = webhookId; }
    }

    public static class Fabrick {
        private boolean enabled = true;
        private String environment = "sandbox";
        private String apiKey;
        private String shopLogin;
        private String baseUrl = "https://sandbox.gestpay.net";
        private String lightboxScriptUrl = "https://sandbox.gestpay.net/pagam/javascript/axerve.js";
        private String notificationUrl;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getShopLogin() { return shopLogin; }
        public void setShopLogin(String shopLogin) { this.shopLogin = shopLogin; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getLightboxScriptUrl() { return lightboxScriptUrl; }
        public void setLightboxScriptUrl(String lightboxScriptUrl) { this.lightboxScriptUrl = lightboxScriptUrl; }
        public String getNotificationUrl() { return notificationUrl; }
        public void setNotificationUrl(String notificationUrl) { this.notificationUrl = notificationUrl; }
    }
}
