package com.newproject.payment.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.newproject.payment.exception.NotFoundException;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Client server-to-server verso order-service per recuperare il total autoritativo dell'ordine.
 * Usato dalla guardia anti-manipolazione importo in {@link PaymentService#create}.
 */
@Component
public class OrderClient {
    private final RestClient orders;

    public OrderClient(@Value("${ecommerce.services.order-base-url}") String orderBaseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.orders = RestClient.builder().baseUrl(orderBaseUrl).requestFactory(factory).build();
    }

    public OrderSummary getSummary(Long orderId) {
        try {
            return orders.get()
                .uri("/api/orders/{id}/summary", orderId)
                .retrieve()
                .body(OrderSummary.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new NotFoundException("Order not found: " + orderId);
            }
            throw new IllegalStateException("order-service summary lookup failed: " + ex.getStatusCode(), ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderSummary {
        private Long id;
        private BigDecimal total;
        private String currency;
        private String status;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public BigDecimal getTotal() {
            return total;
        }

        public void setTotal(BigDecimal total) {
            this.total = total;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
