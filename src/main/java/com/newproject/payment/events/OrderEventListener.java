package com.newproject.payment.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newproject.payment.service.PaymentService;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {
    private static final Logger logger = LoggerFactory.getLogger(OrderEventListener.class);

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    public OrderEventListener(ObjectMapper objectMapper, PaymentService paymentService) {
        this.objectMapper = objectMapper;
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "${payment.order-events.topic:order.events}", groupId = "${spring.application.name}-order-events")
    public void onOrderEvent(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.path("eventType").asText("");
            if (!"ORDER_CREATED".equals(eventType) && !"ORDER_UPDATED".equals(eventType)) {
                return;
            }

            JsonNode orderPayload = root.path("payload");
            Long orderId = asLong(orderPayload.path("id"));
            BigDecimal total = asBigDecimal(orderPayload.path("total"));
            String currency = orderPayload.path("currency").asText("EUR");

            if (orderId == null || total == null) {
                logger.warn("Skipping order event with invalid payload: {}", payload);
                return;
            }

            paymentService.upsertFromOrderEvent(orderId, total, currency);
        } catch (Exception ex) {
            logger.warn("Unable to process order event: {}", ex.getMessage());
        }
    }

    private Long asLong(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            return node.asLong();
        }
        if (node.isTextual()) {
            try {
                return Long.parseLong(node.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private BigDecimal asBigDecimal(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        if (node.isTextual()) {
            try {
                return new BigDecimal(node.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
