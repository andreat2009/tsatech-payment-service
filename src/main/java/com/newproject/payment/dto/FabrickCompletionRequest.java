package com.newproject.payment.dto;

public class FabrickCompletionRequest {
    private String status;
    private String providerPaymentId;
    private String paymentToken;
    private String responseUrl;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProviderPaymentId() { return providerPaymentId; }
    public void setProviderPaymentId(String providerPaymentId) { this.providerPaymentId = providerPaymentId; }
    public String getPaymentToken() { return paymentToken; }
    public void setPaymentToken(String paymentToken) { this.paymentToken = paymentToken; }
    public String getResponseUrl() { return responseUrl; }
    public void setResponseUrl(String responseUrl) { this.responseUrl = responseUrl; }
}
