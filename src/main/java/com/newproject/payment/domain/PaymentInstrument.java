package com.newproject.payment.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "payment_instrument",
    uniqueConstraints = @UniqueConstraint(name = "uk_payment_instrument_customer_fingerprint", columnNames = {"customer_id", "provider_token_fingerprint"})
)
public class PaymentInstrument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "payment_method_code", length = 64, nullable = false)
    private String paymentMethodCode;

    @Column(length = 32, nullable = false)
    private String provider;

    @Column(name = "provider_token_encrypted", columnDefinition = "TEXT", nullable = false)
    private String providerTokenEncrypted;

    @Column(name = "provider_token_fingerprint", length = 128, nullable = false)
    private String providerTokenFingerprint;

    @Column(name = "display_label", length = 255)
    private String displayLabel;

    @Column(length = 64)
    private String brand;

    @Column(length = 4)
    private String last4;

    @Column(name = "expiry_month")
    private Integer expiryMonth;

    @Column(name = "expiry_year")
    private Integer expiryYear;

    @Column(name = "gateway_customer_reference", length = 255)
    private String gatewayCustomerReference;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "default_instrument", nullable = false)
    private Boolean defaultInstrument;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getPaymentMethodCode() { return paymentMethodCode; }
    public void setPaymentMethodCode(String paymentMethodCode) { this.paymentMethodCode = paymentMethodCode; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderTokenEncrypted() { return providerTokenEncrypted; }
    public void setProviderTokenEncrypted(String providerTokenEncrypted) { this.providerTokenEncrypted = providerTokenEncrypted; }
    public String getProviderTokenFingerprint() { return providerTokenFingerprint; }
    public void setProviderTokenFingerprint(String providerTokenFingerprint) { this.providerTokenFingerprint = providerTokenFingerprint; }
    public String getDisplayLabel() { return displayLabel; }
    public void setDisplayLabel(String displayLabel) { this.displayLabel = displayLabel; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getLast4() { return last4; }
    public void setLast4(String last4) { this.last4 = last4; }
    public Integer getExpiryMonth() { return expiryMonth; }
    public void setExpiryMonth(Integer expiryMonth) { this.expiryMonth = expiryMonth; }
    public Integer getExpiryYear() { return expiryYear; }
    public void setExpiryYear(Integer expiryYear) { this.expiryYear = expiryYear; }
    public String getGatewayCustomerReference() { return gatewayCustomerReference; }
    public void setGatewayCustomerReference(String gatewayCustomerReference) { this.gatewayCustomerReference = gatewayCustomerReference; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Boolean getDefaultInstrument() { return defaultInstrument; }
    public void setDefaultInstrument(Boolean defaultInstrument) { this.defaultInstrument = defaultInstrument; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
