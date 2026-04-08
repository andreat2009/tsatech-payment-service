package com.newproject.payment.dto;

public class PaymentInstrumentRequest {
    private String paymentMethodCode;
    private String providerToken;
    private String displayLabel;
    private String brand;
    private String last4;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String gatewayCustomerReference;
    private Boolean active;
    private Boolean defaultInstrument;

    public String getPaymentMethodCode() { return paymentMethodCode; }
    public void setPaymentMethodCode(String paymentMethodCode) { this.paymentMethodCode = paymentMethodCode; }
    public String getProviderToken() { return providerToken; }
    public void setProviderToken(String providerToken) { this.providerToken = providerToken; }
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
}
