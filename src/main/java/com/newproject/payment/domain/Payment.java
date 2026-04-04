package com.newproject.payment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    private String currency;

    @Column(length = 32, nullable = false)
    private String status;

    @Column(length = 64)
    private String provider;

    @Column(name = "method_code", length = 64)
    private String methodCode;

    @Column(name = "method_label", length = 128)
    private String methodLabel;

    @Column(name = "provider_order_id", length = 128)
    private String providerOrderId;

    @Column(name = "provider_payment_id", length = 128)
    private String providerPaymentId;

    @Column(name = "provider_environment", length = 32)
    private String providerEnvironment;

    @Column(name = "provider_status", length = 64)
    private String providerStatus;

    @Column(name = "redirect_url", length = 2000)
    private String redirectUrl;

    @Column(name = "approval_url", length = 2000)
    private String approvalUrl;

    @Column(name = "return_url", length = 2000)
    private String returnUrl;

    @Column(name = "cancel_url", length = 2000)
    private String cancelUrl;

    @Column(name = "failure_code", length = 128)
    private String failureCode;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "lightbox_script_url", length = 2000)
    private String lightboxScriptUrl;

    @Column(name = "lightbox_shop_login", length = 128)
    private String lightboxShopLogin;

    @Column(name = "lightbox_payment_token", length = 255)
    private String lightboxPaymentToken;

    @Column(name = "refunded_amount", nullable = false, precision = 15, scale = 4)
    private BigDecimal refundedAmount;

    @Column(name = "last_reconciled_at")
    private OffsetDateTime lastReconciledAt;

    @Column(name = "last_webhook_at")
    private OffsetDateTime lastWebhookAt;

    @Column(name = "last_provider_sync_at")
    private OffsetDateTime lastProviderSyncAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
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
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getLightboxScriptUrl() { return lightboxScriptUrl; }
    public void setLightboxScriptUrl(String lightboxScriptUrl) { this.lightboxScriptUrl = lightboxScriptUrl; }
    public String getLightboxShopLogin() { return lightboxShopLogin; }
    public void setLightboxShopLogin(String lightboxShopLogin) { this.lightboxShopLogin = lightboxShopLogin; }
    public String getLightboxPaymentToken() { return lightboxPaymentToken; }
    public void setLightboxPaymentToken(String lightboxPaymentToken) { this.lightboxPaymentToken = lightboxPaymentToken; }
    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public void setRefundedAmount(BigDecimal refundedAmount) { this.refundedAmount = refundedAmount; }
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
