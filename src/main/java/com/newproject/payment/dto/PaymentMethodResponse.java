package com.newproject.payment.dto;

public class PaymentMethodResponse {
    private Long id;
    private String code;
    private String displayName;
    private String provider;
    private String paymentFlow;
    private String description;
    private Boolean active;
    private Integer sortOrder;
    private String providerBrandName;
    private String providerLightboxScriptUrl;
    private Boolean providerConfigurationAvailable;

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
    public String getProviderBrandName() { return providerBrandName; }
    public void setProviderBrandName(String providerBrandName) { this.providerBrandName = providerBrandName; }
    public String getProviderLightboxScriptUrl() { return providerLightboxScriptUrl; }
    public void setProviderLightboxScriptUrl(String providerLightboxScriptUrl) { this.providerLightboxScriptUrl = providerLightboxScriptUrl; }
    public Boolean getProviderConfigurationAvailable() { return providerConfigurationAvailable; }
    public void setProviderConfigurationAvailable(Boolean providerConfigurationAvailable) { this.providerConfigurationAvailable = providerConfigurationAvailable; }
}
