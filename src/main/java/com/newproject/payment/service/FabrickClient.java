package com.newproject.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newproject.payment.domain.Payment;
import com.newproject.payment.exception.BadRequestException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class FabrickClient {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public FabrickClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    public boolean isAvailable(PaymentMethodProviderConfigurationResolver.ResolvedFabrickConfig config) {
        return config != null && config.isAvailable();
    }

    public FabrickCreateResult createHostedPayment(PaymentMethodProviderConfigurationResolver.ResolvedFabrickConfig config, Payment payment) {
        if (!isAvailable(config)) {
            throw new BadRequestException("Fabrick credentials are not configured");
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("shopLogin", config.shopLogin());
            body.put("amount", formatAmount(payment.getAmount()));
            body.put("currency", payment.getCurrency());
            body.put("shopTransactionID", buildShopTransactionId(payment));
            if (notBlank(payment.getReturnUrl()) || notBlank(payment.getCancelUrl()) || notBlank(config.notificationUrl())) {
                Map<String, Object> responseUrls = new LinkedHashMap<>();
                if (notBlank(payment.getReturnUrl())) {
                    responseUrls.put("buyerOK", payment.getReturnUrl());
                }
                if (notBlank(payment.getCancelUrl())) {
                    responseUrls.put("buyerKO", payment.getCancelUrl());
                }
                if (notBlank(config.notificationUrl())) {
                    responseUrls.put(
                        "serverNotificationURL",
                        appendQueryParam(
                            appendQueryParam(config.notificationUrl(), "paymentId", String.valueOf(payment.getId())),
                            "orderId",
                            String.valueOf(payment.getOrderId())
                        )
                    );
                } else if (notBlank(payment.getReturnUrl())) {
                    responseUrls.put("serverNotificationURL", payment.getReturnUrl());
                }
                body.put("responseURLs", responseUrls);
            }

            HttpRequest request = HttpRequest.newBuilder(URI.create(trimTrailingSlash(config.baseUrl()) + "/api/v1/payment/create/"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "apikey " + config.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ensureSuccess(response, "Fabrick payment creation");

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode error = root.path("error");
            if (!error.isMissingNode() && error.path("code").asInt(0) != 0) {
                throw new BadRequestException("Fabrick error " + error.path("code").asText() + ": " + error.path("description").asText());
            }
            JsonNode payload = root.path("payload");
            String paymentId = payload.path("paymentID").asText(null);
            String paymentToken = payload.path("paymentToken").asText(null);
            String redirectUrl = payload.path("userRedirect").path("href").asText(null);
            if (paymentId == null || paymentToken == null) {
                throw new BadRequestException("Fabrick did not return paymentID/paymentToken");
            }
            return new FabrickCreateResult(
                paymentId,
                paymentToken,
                redirectUrl,
                config.lightboxScriptUrl(),
                config.shopLogin()
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Unable to reach Fabrick: " + ex.getMessage());
        } catch (IOException ex) {
            throw new BadRequestException("Unable to reach Fabrick: " + ex.getMessage());
        }
    }

    public FabrickPaymentDetail fetchPaymentDetail(PaymentMethodProviderConfigurationResolver.ResolvedFabrickConfig config, String providerPaymentId, String paymentToken, String shopTransactionId) {
        if (!isAvailable(config)) {
            throw new BadRequestException("Fabrick credentials are not configured");
        }
        if (!notBlank(providerPaymentId) && !notBlank(shopTransactionId)) {
            throw new BadRequestException("Fabrick payment detail requires paymentID or shopTransactionID");
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("shopLogin", config.shopLogin());
            if (notBlank(providerPaymentId)) {
                body.put("paymentID", providerPaymentId);
            }
            if (notBlank(shopTransactionId)) {
                body.put("shopTransactionID", shopTransactionId);
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(trimTrailingSlash(config.baseUrl()) + "/api/v1/payment/detail"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "apikey " + config.apiKey())
                .header("Content-Type", "application/json");
            if (notBlank(paymentToken)) {
                requestBuilder.header("paymentToken", paymentToken);
            }

            HttpRequest request = requestBuilder
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ensureSuccess(response, "Fabrick payment detail");

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode error = root.path("error");
            if (!error.isMissingNode() && error.path("code").asInt(0) != 0) {
                throw new BadRequestException("Fabrick error " + error.path("code").asText() + ": " + error.path("description").asText());
            }
            JsonNode payload = root.path("payload");
            return new FabrickPaymentDetail(
                firstText(payload.path("paymentID"), root.path("paymentID"), textNode(providerPaymentId)),
                firstText(payload.path("shopTransactionID"), root.path("shopTransactionID"), textNode(shopTransactionId)),
                firstText(payload.path("transactionResult"), root.path("transactionResult")),
                firstText(payload.path("transactionState"), root.path("transactionState")),
                firstText(payload.path("errorCode"), root.path("errorCode"), error.path("code")),
                firstText(payload.path("errorDescription"), root.path("errorDescription"), error.path("description"))
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Unable to query Fabrick payment detail: " + ex.getMessage());
        } catch (IOException ex) {
            throw new BadRequestException("Unable to query Fabrick payment detail: " + ex.getMessage());
        }
    }

    private void ensureSuccess(HttpResponse<String> response, String operation) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return;
        }
        throw new BadRequestException(operation + " failed with HTTP " + response.statusCode() + ": " + response.body());
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String buildShopTransactionId(Payment payment) {
        return "ORDER-" + payment.getOrderId() + "-PAY-" + payment.getId();
    }

    private String appendQueryParam(String url, String key, String value) {
        if (!notBlank(url)) {
            return url;
        }
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + key + '=' + value;
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

    private String firstText(JsonNode... nodes) {
        if (nodes == null) {
            return null;
        }
        for (JsonNode node : nodes) {
            if (node == null || node.isMissingNode() || node.isNull()) {
                continue;
            }
            String text = node.isTextual() ? node.asText() : node.toString();
            if (text != null && !text.isBlank() && !"null".equalsIgnoreCase(text)) {
                return text;
            }
        }
        return null;
    }

    private JsonNode textNode(String value) {
        return value == null ? objectMapper.nullNode() : objectMapper.getNodeFactory().textNode(value);
    }

    public record FabrickCreateResult(String paymentId, String paymentToken, String redirectUrl, String scriptUrl, String shopLogin) {}
    public record FabrickPaymentDetail(String paymentId, String shopTransactionId, String transactionResult, String transactionState, String errorCode, String errorDescription) {}
}
