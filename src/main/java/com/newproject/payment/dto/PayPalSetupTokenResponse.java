package com.newproject.payment.dto;

public class PayPalSetupTokenResponse {
    private String paymentMethodCode;
    private String setupToken;
    private String providerCustomerId;
    private String status;

    public String getPaymentMethodCode() { return paymentMethodCode; }
    public void setPaymentMethodCode(String paymentMethodCode) { this.paymentMethodCode = paymentMethodCode; }
    public String getSetupToken() { return setupToken; }
    public void setSetupToken(String setupToken) { this.setupToken = setupToken; }
    public String getProviderCustomerId() { return providerCustomerId; }
    public void setProviderCustomerId(String providerCustomerId) { this.providerCustomerId = providerCustomerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
