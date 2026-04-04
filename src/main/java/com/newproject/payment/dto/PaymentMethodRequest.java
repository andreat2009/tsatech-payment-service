package com.newproject.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PaymentMethodRequest {
    @NotBlank
    private String code;
    @NotBlank
    private String displayName;
    @NotBlank
    private String provider;
    @NotBlank
    private String paymentFlow;
    private String description;
    private Boolean active;
    @NotNull
    private Integer sortOrder;

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
}
