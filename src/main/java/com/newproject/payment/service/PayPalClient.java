package com.newproject.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newproject.payment.domain.Payment;
import com.newproject.payment.dto.PaymentRequest;
import com.newproject.payment.exception.BadRequestException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PayPalClient {
    private final PaymentProviderProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public PayPalClient(PaymentProviderProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    public boolean isAvailable() {
        return properties.getPaypal().isEnabled()
            && notBlank(properties.getPaypal().getClientId())
            && notBlank(properties.getPaypal().getClientSecret())
            && notBlank(properties.getPaypal().getBaseUrl());
    }

    public boolean isWebhookVerificationAvailable() {
        return isAvailable() && notBlank(properties.getPaypal().getWebhookId());
    }

    public PayPalCreateResult createOrder(Payment payment, PaymentRequest request) {
        if (!isAvailable()) {
            throw new BadRequestException("PayPal sandbox/production credentials are not configured");
        }
        try {
            String accessToken = obtainAccessToken();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("intent", "CAPTURE");
            body.put("purchase_units", List.of(Map.of(
                "reference_id", "ORDER-" + request.getOrderId(),
                "custom_id", "PAYMENT-" + payment.getId(),
                "amount", Map.of(
                    "currency_code", request.getCurrency(),
                    "value", formatAmount(request.getAmount())
                ),
                "description", "Order #" + request.getOrderId()
            )));
            body.put("payment_source", Map.of(
                "paypal", Map.of(
                    "experience_context", Map.of(
                        "brand_name", properties.getPaypal().getBrandName(),
                        "shipping_preference", "NO_SHIPPING",
                        "user_action", "PAY_NOW",
                        "return_url", request.getReturnUrl(),
                        "cancel_url", request.getCancelUrl()
                    )
                )
            ));

            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(trimTrailingSlash(properties.getPaypal().getBaseUrl()) + "/v2/checkout/orders"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            ensureSuccess(response, "PayPal create order");
            JsonNode root = objectMapper.readTree(response.body());
            String orderId = root.path("id").asText(null);
            String status = root.path("status").asText(null);
            String approvalUrl = null;
            for (JsonNode link : root.path("links")) {
                String rel = link.path("rel").asText();
                if ("payer-action".equalsIgnoreCase(rel) || "approve".equalsIgnoreCase(rel)) {
                    approvalUrl = link.path("href").asText(null);
                    break;
                }
            }
            if (orderId == null || approvalUrl == null) {
                throw new BadRequestException("PayPal did not return an approval URL");
            }
            return new PayPalCreateResult(orderId, approvalUrl, status);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Unable to reach PayPal: " + ex.getMessage());
        } catch (IOException ex) {
            throw new BadRequestException("Unable to reach PayPal: " + ex.getMessage());
        }
    }

    public PayPalCaptureResult captureOrder(Payment payment, String token) {
        if (!isAvailable()) {
            throw new BadRequestException("PayPal sandbox/production credentials are not configured");
        }
        String paypalOrderId = notBlank(token) ? token : payment.getProviderOrderId();
        if (!notBlank(paypalOrderId)) {
            throw new BadRequestException("PayPal order token is missing");
        }
        try {
            String accessToken = obtainAccessToken();
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(trimTrailingSlash(properties.getPaypal().getBaseUrl()) + "/v2/checkout/orders/" + paypalOrderId + "/capture"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            ensureSuccess(response, "PayPal capture order");
            JsonNode root = objectMapper.readTree(response.body());
            String captureStatus = root.path("status").asText(null);
            String captureId = null;
            JsonNode purchaseUnits = root.path("purchase_units");
            if (purchaseUnits.isArray() && purchaseUnits.size() > 0) {
                JsonNode captures = purchaseUnits.get(0).path("payments").path("captures");
                if (captures.isArray() && captures.size() > 0) {
                    captureId = captures.get(0).path("id").asText(null);
                    if (captureStatus == null || captureStatus.isBlank()) {
                        captureStatus = captures.get(0).path("status").asText(null);
                    }
                }
            }
            return new PayPalCaptureResult(paypalOrderId, captureId, captureStatus);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Unable to capture PayPal payment: " + ex.getMessage());
        } catch (IOException ex) {
            throw new BadRequestException("Unable to capture PayPal payment: " + ex.getMessage());
        }
    }

    public PayPalRefundResult refundCapture(String captureId, BigDecimal amount, String currency, String reason, String requestId) {
        if (!isAvailable()) {
            throw new BadRequestException("PayPal sandbox/production credentials are not configured");
        }
        if (!notBlank(captureId)) {
            throw new BadRequestException("PayPal capture id is missing");
        }
        try {
            String accessToken = obtainAccessToken();
            Map<String, Object> body = new LinkedHashMap<>();
            if (amount != null) {
                body.put("amount", Map.of(
                    "currency_code", currency,
                    "value", formatAmount(amount)
                ));
            }
            if (notBlank(reason)) {
                body.put("note_to_payer", reason);
            }

            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(trimTrailingSlash(properties.getPaypal().getBaseUrl()) + "/v2/payments/captures/" + captureId + "/refund"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("PayPal-Request-Id", notBlank(requestId) ? requestId : UUID.randomUUID().toString())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            ensureSuccess(response, "PayPal refund capture");
            JsonNode root = objectMapper.readTree(response.body());
            return new PayPalRefundResult(
                root.path("id").asText(null),
                root.path("status").asText(null),
                parseAmount(root.path("amount").path("value"))
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Unable to refund PayPal capture: " + ex.getMessage());
        } catch (IOException ex) {
            throw new BadRequestException("Unable to refund PayPal capture: " + ex.getMessage());
        }
    }

    public PayPalOrderSnapshot fetchOrderSnapshot(String providerOrderId) {
        if (!isAvailable()) {
            throw new BadRequestException("PayPal sandbox/production credentials are not configured");
        }
        if (!notBlank(providerOrderId)) {
            throw new BadRequestException("PayPal order id is missing");
        }
        try {
            String accessToken = obtainAccessToken();
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(trimTrailingSlash(properties.getPaypal().getBaseUrl()) + "/v2/checkout/orders/" + providerOrderId))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            ensureSuccess(response, "PayPal fetch order");
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode firstCapture = null;
            JsonNode purchaseUnits = root.path("purchase_units");
            if (purchaseUnits.isArray() && purchaseUnits.size() > 0) {
                JsonNode captures = purchaseUnits.get(0).path("payments").path("captures");
                if (captures.isArray() && captures.size() > 0) {
                    firstCapture = captures.get(0);
                }
            }
            BigDecimal refundedAmount = BigDecimal.ZERO;
            if (firstCapture != null) {
                JsonNode refunds = firstCapture.path("refunds");
                if (refunds.isArray()) {
                    for (JsonNode refund : refunds) {
                        refundedAmount = refundedAmount.add(parseAmount(refund.path("amount").path("value")));
                    }
                }
            }
            return new PayPalOrderSnapshot(
                root.path("id").asText(null),
                root.path("status").asText(null),
                firstCapture != null ? firstCapture.path("id").asText(null) : null,
                firstCapture != null ? firstCapture.path("status").asText(null) : null,
                refundedAmount
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Unable to fetch PayPal order snapshot: " + ex.getMessage());
        } catch (IOException ex) {
            throw new BadRequestException("Unable to fetch PayPal order snapshot: " + ex.getMessage());
        }
    }

    public boolean verifyWebhookSignature(
        String transmissionId,
        String transmissionTime,
        String certUrl,
        String authAlgo,
        String transmissionSig,
        String requestBody
    ) {
        if (!isWebhookVerificationAvailable()) {
            throw new BadRequestException("PayPal webhook verification is not available. Configure PAYMENT_PAYPAL_WEBHOOK_ID first.");
        }
        try {
            String accessToken = obtainAccessToken();
            Map<String, Object> verificationRequest = new LinkedHashMap<>();
            verificationRequest.put("transmission_id", transmissionId);
            verificationRequest.put("transmission_time", transmissionTime);
            verificationRequest.put("cert_url", certUrl);
            verificationRequest.put("auth_algo", authAlgo);
            verificationRequest.put("transmission_sig", transmissionSig);
            verificationRequest.put("webhook_id", properties.getPaypal().getWebhookId());
            verificationRequest.put("webhook_event", objectMapper.readTree(requestBody));

            HttpRequest request = HttpRequest.newBuilder(URI.create(trimTrailingSlash(properties.getPaypal().getBaseUrl()) + "/v1/notifications/verify-webhook-signature"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(verificationRequest)))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ensureSuccess(response, "PayPal verify webhook signature");
            JsonNode root = objectMapper.readTree(response.body());
            return "SUCCESS".equalsIgnoreCase(root.path("verification_status").asText());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Unable to verify PayPal webhook signature: " + ex.getMessage());
        } catch (IOException ex) {
            throw new BadRequestException("Unable to verify PayPal webhook signature: " + ex.getMessage());
        }
    }

    private String obtainAccessToken() throws IOException, InterruptedException {
        String credentials = properties.getPaypal().getClientId() + ":" + properties.getPaypal().getClientSecret();
        String basic = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        HttpRequest tokenRequest = HttpRequest.newBuilder(URI.create(trimTrailingSlash(properties.getPaypal().getBaseUrl()) + "/v1/oauth2/token"))
            .timeout(Duration.ofSeconds(15))
            .header("Authorization", "Basic " + basic)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
            .build();
        HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(tokenResponse, "PayPal access token");
        JsonNode tokenRoot = objectMapper.readTree(tokenResponse.body());
        String accessToken = tokenRoot.path("access_token").asText(null);
        if (accessToken == null || accessToken.isBlank()) {
            throw new BadRequestException("PayPal did not return an access token");
        }
        return accessToken;
    }

    private BigDecimal parseAmount(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return BigDecimal.ZERO;
        }
        String value = node.asText(null);
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private void ensureSuccess(HttpResponse<String> response, String operation) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return;
        }
        throw new BadRequestException("" + operation + " failed with HTTP " + response.statusCode() + ": " + response.body());
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String trimTrailingSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    public record PayPalCreateResult(String orderId, String approvalUrl, String status) {}
    public record PayPalCaptureResult(String orderId, String captureId, String status) {}
    public record PayPalRefundResult(String refundId, String status, BigDecimal refundedAmount) {}
    public record PayPalOrderSnapshot(String orderId, String orderStatus, String captureId, String captureStatus, BigDecimal refundedAmount) {}
}
