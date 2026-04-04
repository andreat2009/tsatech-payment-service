package com.newproject.payment.dto;

public class AdminPaymentMethodResponse extends PaymentMethodResponse {
    private String providerEnvironment;
    private String providerBaseUrl;
    private String providerBrandName;
    private String providerWebhookId;
    private String providerClientId;
    private Boolean providerClientSecretConfigured;
    private String providerClientSecretSource;
    private String providerShopLogin;
    private Boolean providerApiKeyConfigured;
    private String providerApiKeySource;
    private String providerLightboxScriptUrl;
    private String providerNotificationUrl;
    private Boolean providerConfigurationAvailable;

    public String getProviderEnvironment() { return providerEnvironment; }
    public void setProviderEnvironment(String providerEnvironment) { this.providerEnvironment = providerEnvironment; }
    public String getProviderBaseUrl() { return providerBaseUrl; }
    public void setProviderBaseUrl(String providerBaseUrl) { this.providerBaseUrl = providerBaseUrl; }
    public String getProviderBrandName() { return providerBrandName; }
    public void setProviderBrandName(String providerBrandName) { this.providerBrandName = providerBrandName; }
    public String getProviderWebhookId() { return providerWebhookId; }
    public void setProviderWebhookId(String providerWebhookId) { this.providerWebhookId = providerWebhookId; }
    public String getProviderClientId() { return providerClientId; }
    public void setProviderClientId(String providerClientId) { this.providerClientId = providerClientId; }
    public Boolean getProviderClientSecretConfigured() { return providerClientSecretConfigured; }
    public void setProviderClientSecretConfigured(Boolean providerClientSecretConfigured) { this.providerClientSecretConfigured = providerClientSecretConfigured; }
    public String getProviderClientSecretSource() { return providerClientSecretSource; }
    public void setProviderClientSecretSource(String providerClientSecretSource) { this.providerClientSecretSource = providerClientSecretSource; }
    public String getProviderShopLogin() { return providerShopLogin; }
    public void setProviderShopLogin(String providerShopLogin) { this.providerShopLogin = providerShopLogin; }
    public Boolean getProviderApiKeyConfigured() { return providerApiKeyConfigured; }
    public void setProviderApiKeyConfigured(Boolean providerApiKeyConfigured) { this.providerApiKeyConfigured = providerApiKeyConfigured; }
    public String getProviderApiKeySource() { return providerApiKeySource; }
    public void setProviderApiKeySource(String providerApiKeySource) { this.providerApiKeySource = providerApiKeySource; }
    public String getProviderLightboxScriptUrl() { return providerLightboxScriptUrl; }
    public void setProviderLightboxScriptUrl(String providerLightboxScriptUrl) { this.providerLightboxScriptUrl = providerLightboxScriptUrl; }
    public String getProviderNotificationUrl() { return providerNotificationUrl; }
    public void setProviderNotificationUrl(String providerNotificationUrl) { this.providerNotificationUrl = providerNotificationUrl; }
    public Boolean getProviderConfigurationAvailable() { return providerConfigurationAvailable; }
    public void setProviderConfigurationAvailable(Boolean providerConfigurationAvailable) { this.providerConfigurationAvailable = providerConfigurationAvailable; }
}
