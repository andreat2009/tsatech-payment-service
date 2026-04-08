package com.newproject.payment.dto;

import java.time.OffsetDateTime;

public class PaymentInstrumentResponse {
    private Long id;
    private Long customerId;
    private String paymentMethodCode;
    private String provider;
    private String displayLabel;
    private String brand;
    private String last4;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String gatewayCustomerReference;
    private Boolean active;
    private Boolean defaultInstrument;
    private Boolean tokenStored;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getPaymentMethodCode() { return paymentMethodCode; }
    public void setPaymentMethodCode(String paymentMethodCode) { this.paymentMethodCode = paymentMethodCode; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
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
    public Boolean getTokenStored() { return tokenStored; }
    public void setTokenStored(Boolean tokenStored) { this.tokenStored = tokenStored; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
