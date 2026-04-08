package com.newproject.payment.dto;

public class PayPalBrowserVaultSessionResponse {
    private String paymentMethodCode;
    private String displayName;
    private String provider;
    private String providerBrandName;
    private String clientId;
    private String userIdToken;
    private String sdkUrl;

    public String getPaymentMethodCode() { return paymentMethodCode; }
    public void setPaymentMethodCode(String paymentMethodCode) { this.paymentMethodCode = paymentMethodCode; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderBrandName() { return providerBrandName; }
    public void setProviderBrandName(String providerBrandName) { this.providerBrandName = providerBrandName; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getUserIdToken() { return userIdToken; }
    public void setUserIdToken(String userIdToken) { this.userIdToken = userIdToken; }
    public String getSdkUrl() { return sdkUrl; }
    public void setSdkUrl(String sdkUrl) { this.sdkUrl = sdkUrl; }
}
