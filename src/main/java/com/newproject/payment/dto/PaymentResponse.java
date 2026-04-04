package com.newproject.payment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class PaymentResponse {
    private Long id;
    private Long orderId;
    private BigDecimal amount;
    private BigDecimal refundedAmount;
    private String currency;
    private String status;
    private String provider;
    private String methodCode;
    private String methodLabel;
    private String providerOrderId;
    private String providerPaymentId;
    private String providerEnvironment;
    private String providerStatus;
    private String redirectUrl;
    private String approvalUrl;
    private String returnUrl;
    private String cancelUrl;
    private String failureCode;
    private String failureReason;
    private String lightboxScriptUrl;
    private String lightboxShopLogin;
    private String lightboxPaymentToken;
    private OffsetDateTime lastReconciledAt;
    private OffsetDateTime lastWebhookAt;
    private OffsetDateTime lastProviderSyncAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public void setRefundedAmount(BigDecimal refundedAmount) { this.refundedAmount = refundedAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getMethodCode() { return methodCode; }
    public void setMethodCode(String methodCode) { this.methodCode = methodCode; }
    public String getMethodLabel() { return methodLabel; }
    public void setMethodLabel(String methodLabel) { this.methodLabel = methodLabel; }
    public String getProviderOrderId() { return providerOrderId; }
    public void setProviderOrderId(String providerOrderId) { this.providerOrderId = providerOrderId; }
    public String getProviderPaymentId() { return providerPaymentId; }
    public void setProviderPaymentId(String providerPaymentId) { this.providerPaymentId = providerPaymentId; }
    public String getProviderEnvironment() { return providerEnvironment; }
    public void setProviderEnvironment(String providerEnvironment) { this.providerEnvironment = providerEnvironment; }
    public String getProviderStatus() { return providerStatus; }
    public void setProviderStatus(String providerStatus) { this.providerStatus = providerStatus; }
    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }
    public String getApprovalUrl() { return approvalUrl; }
    public void setApprovalUrl(String approvalUrl) { this.approvalUrl = approvalUrl; }
    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
    public String getCancelUrl() { return cancelUrl; }
    public void setCancelUrl(String cancelUrl) { this.cancelUrl = cancelUrl; }
    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getLightboxScriptUrl() { return lightboxScriptUrl; }
    public void setLightboxScriptUrl(String lightboxScriptUrl) { this.lightboxScriptUrl = lightboxScriptUrl; }
    public String getLightboxShopLogin() { return lightboxShopLogin; }
    public void setLightboxShopLogin(String lightboxShopLogin) { this.lightboxShopLogin = lightboxShopLogin; }
    public String getLightboxPaymentToken() { return lightboxPaymentToken; }
    public void setLightboxPaymentToken(String lightboxPaymentToken) { this.lightboxPaymentToken = lightboxPaymentToken; }
    public OffsetDateTime getLastReconciledAt() { return lastReconciledAt; }
    public void setLastReconciledAt(OffsetDateTime lastReconciledAt) { this.lastReconciledAt = lastReconciledAt; }
    public OffsetDateTime getLastWebhookAt() { return lastWebhookAt; }
    public void setLastWebhookAt(OffsetDateTime lastWebhookAt) { this.lastWebhookAt = lastWebhookAt; }
    public OffsetDateTime getLastProviderSyncAt() { return lastProviderSyncAt; }
    public void setLastProviderSyncAt(OffsetDateTime lastProviderSyncAt) { this.lastProviderSyncAt = lastProviderSyncAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
