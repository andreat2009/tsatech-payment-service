package com.newproject.payment.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "payment_method")
public class PaymentMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false, unique = true)
    private String code;

    @Column(name = "display_name", length = 128, nullable = false)
    private String displayName;

    @Column(length = 32, nullable = false)
    private String provider;

    @Column(name = "payment_flow", length = 32, nullable = false)
    private String paymentFlow;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "provider_environment", length = 32)
    private String providerEnvironment;

    @Column(name = "provider_base_url", length = 512)
    private String providerBaseUrl;

    @Column(name = "provider_brand_name", length = 255)
    private String providerBrandName;

    @Column(name = "provider_webhook_id", length = 255)
    private String providerWebhookId;

    @Column(name = "provider_client_id", length = 512)
    private String providerClientId;

    @Column(name = "provider_client_secret_encrypted", columnDefinition = "TEXT")
    private String providerClientSecretEncrypted;

    @Column(name = "provider_shop_login", length = 255)
    private String providerShopLogin;

    @Column(name = "provider_api_key_encrypted", columnDefinition = "TEXT")
    private String providerApiKeyEncrypted;

    @Column(name = "provider_lightbox_script_url", length = 1024)
    private String providerLightboxScriptUrl;

    @Column(name = "provider_notification_url", length = 1024)
    private String providerNotificationUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getPaymentFlow() { return paymentFlow; }
    public void setPaymentFlow(String paymentFlow) { this.paymentFlow = paymentFlow; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
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
    public String getProviderClientSecretEncrypted() { return providerClientSecretEncrypted; }
    public void setProviderClientSecretEncrypted(String providerClientSecretEncrypted) { this.providerClientSecretEncrypted = providerClientSecretEncrypted; }
    public String getProviderShopLogin() { return providerShopLogin; }
    public void setProviderShopLogin(String providerShopLogin) { this.providerShopLogin = providerShopLogin; }
    public String getProviderApiKeyEncrypted() { return providerApiKeyEncrypted; }
    public void setProviderApiKeyEncrypted(String providerApiKeyEncrypted) { this.providerApiKeyEncrypted = providerApiKeyEncrypted; }
    public String getProviderLightboxScriptUrl() { return providerLightboxScriptUrl; }
    public void setProviderLightboxScriptUrl(String providerLightboxScriptUrl) { this.providerLightboxScriptUrl = providerLightboxScriptUrl; }
    public String getProviderNotificationUrl() { return providerNotificationUrl; }
    public void setProviderNotificationUrl(String providerNotificationUrl) { this.providerNotificationUrl = providerNotificationUrl; }
}
