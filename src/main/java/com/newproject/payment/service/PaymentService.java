package com.newproject.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newproject.payment.domain.Payment;
import com.newproject.payment.domain.PaymentMethod;
import com.newproject.payment.domain.PaymentTransaction;
import com.newproject.payment.dto.AdminPaymentMethodResponse;
import com.newproject.payment.dto.FabrickCompletionRequest;
import com.newproject.payment.dto.PaymentMethodRequest;
import com.newproject.payment.dto.PaymentMethodResponse;
import com.newproject.payment.dto.PaymentRefundRequest;
import com.newproject.payment.dto.PaymentRequest;
import com.newproject.payment.dto.PaymentResponse;
import com.newproject.payment.events.EventPublisher;
import com.newproject.payment.exception.BadRequestException;
import com.newproject.payment.exception.NotFoundException;
import com.newproject.payment.repository.PaymentMethodRepository;
import com.newproject.payment.repository.PaymentRepository;
import com.newproject.payment.repository.PaymentTransactionRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final EventPublisher eventPublisher;
    private final PayPalClient payPalClient;
    private final FabrickClient fabrickClient;
    private final PaymentMethodProviderConfigurationResolver providerConfigurationResolver;
    private final PaymentCredentialCryptoService paymentCredentialCryptoService;
    private final ObjectMapper objectMapper;

    public PaymentService(
        PaymentRepository paymentRepository,
        PaymentMethodRepository paymentMethodRepository,
        PaymentTransactionRepository paymentTransactionRepository,
        EventPublisher eventPublisher,
        PayPalClient payPalClient,
        FabrickClient fabrickClient,
        PaymentMethodProviderConfigurationResolver providerConfigurationResolver,
        PaymentCredentialCryptoService paymentCredentialCryptoService,
        ObjectMapper objectMapper
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.eventPublisher = eventPublisher;
        this.payPalClient = payPalClient;
        this.fabrickClient = fabrickClient;
        this.providerConfigurationResolver = providerConfigurationResolver;
        this.paymentCredentialCryptoService = paymentCredentialCryptoService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> listMethods() {
        return paymentMethodRepository.findByActiveTrueOrderBySortOrderAscCodeAsc().stream()
            .filter(this::isMethodAvailable)
            .map(this::toMethodResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminPaymentMethodResponse> listAdminMethods() {
        return paymentMethodRepository.findAllByOrderBySortOrderAscCodeAsc().stream()
            .map(this::toAdminMethodResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public AdminPaymentMethodResponse getAdminMethod(Long id) {
        return toAdminMethodResponse(findMethodById(id));
    }

    @Transactional
    public AdminPaymentMethodResponse createAdminMethod(PaymentMethodRequest request) {
        PaymentMethod method = new PaymentMethod();
        applyMethodRequest(method, request);
        return toAdminMethodResponse(paymentMethodRepository.save(method));
    }

    @Transactional
    public AdminPaymentMethodResponse updateAdminMethod(Long id, PaymentMethodRequest request) {
        PaymentMethod method = findMethodById(id);
        applyMethodRequest(method, request);
        return toAdminMethodResponse(paymentMethodRepository.save(method));
    }

    @Transactional
    public void deleteAdminMethod(Long id) {
        paymentMethodRepository.delete(findMethodById(id));
    }

    @Transactional
    public PaymentResponse create(PaymentRequest request) {
        String methodCode = normalizeMethodCode(request.getMethodCode(), request.getProvider());
        PaymentMethod method = resolveMethod(methodCode);

        Payment payment = paymentRepository.findFirstByOrderIdOrderByIdAsc(request.getOrderId()).orElseGet(Payment::new);
        boolean created = payment.getId() == null;
        OffsetDateTime now = OffsetDateTime.now();

        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(normalizeCurrency(request.getCurrency()));
        payment.setMethodCode(method.getCode());
        payment.setMethodLabel(method.getDisplayName());
        payment.setProvider(method.getProvider());
        payment.setIdempotencyKey(trimToNull(request.getIdempotencyKey()));
        payment.setFailureCode(null);
        payment.setFailureReason(null);
        payment.setRedirectUrl(null);
        payment.setApprovalUrl(null);
        payment.setLightboxPaymentToken(null);
        payment.setLightboxScriptUrl(null);
        payment.setLightboxShopLogin(null);
        payment.setProviderOrderId(null);
        payment.setProviderPaymentId(null);
        payment.setProviderStatus(null);
        payment.setRefundedAmount(BigDecimal.ZERO);
        payment.setLastReconciledAt(null);
        payment.setLastWebhookAt(null);
        payment.setLastProviderSyncAt(null);
        if (created) {
            payment.setCreatedAt(now);
        }
        payment.setUpdatedAt(now);
        payment = paymentRepository.save(payment);
        payment.setReturnUrl(appendQueryParamIfMissing(trimToNull(request.getReturnUrl()), "paymentId", String.valueOf(payment.getId())));
        payment.setCancelUrl(appendQueryParamIfMissing(trimToNull(request.getCancelUrl()), "paymentId", String.valueOf(payment.getId())));

        initiatePayment(payment, request, method);

        Payment saved = paymentRepository.save(payment);
        recordPaymentTransaction(saved, "INITIATE", "PAYMENT_CREATE", serializePayload(Map.of(
            "provider", saved.getProvider(),
            "methodCode", saved.getMethodCode(),
            "redirectUrl", saved.getRedirectUrl() == null ? "" : saved.getRedirectUrl()
        )), saved.getAmount(), firstNonBlank(saved.getProviderPaymentId(), saved.getProviderOrderId()));
        publishPaymentEvent(created ? "PAYMENT_CREATED" : "PAYMENT_UPDATED", saved);
        return toResponse(saved);
    }

    @Transactional
    public PaymentResponse capturePayPal(Long id, String token) {
        Payment payment = findPayment(id);
        ensureProvider(payment, "PAYPAL");

        PaymentMethod method = resolveMethodForPayment(payment);
        PayPalClient.PayPalCaptureResult capture = payPalClient.captureOrder(resolvePayPalConfig(method), payment, token);
        return persistPayPalState(
            payment,
            capture.orderId(),
            capture.captureId(),
            capture.status(),
            "PAYPAL_CAPTURE",
            "CAPTURE",
            serializePayload(capture),
            payment.getAmount(),
            true,
            false,
            false
        );
    }

    @Transactional
    public PaymentResponse completeFabrick(Long id, FabrickCompletionRequest request) {
        Payment payment = findPayment(id);
        ensureProvider(payment, "FABRICK");

        String providerPaymentId = firstNonBlank(trimToNull(request.getProviderPaymentId()), payment.getProviderPaymentId());
        String paymentToken = firstNonBlank(trimToNull(request.getPaymentToken()), payment.getLightboxPaymentToken());
        PaymentMethod method = resolveMethodForPayment(payment);
        FabrickClient.FabrickPaymentDetail detail = fabrickClient.fetchPaymentDetail(resolveFabrickConfig(method), providerPaymentId, paymentToken, buildShopTransactionId(payment));
        if (trimToNull(request.getResponseUrl()) != null) {
            payment.setRedirectUrl(trimToNull(request.getResponseUrl()));
        }
        return persistFabrickState(
            payment,
            detail,
            "FABRICK_COMPLETE",
            "COMPLETE",
            serializePayload(detail),
            payment.getAmount(),
            true,
            false,
            false
        );
    }

    @Transactional
    public PaymentResponse refund(Long id, PaymentRefundRequest request) {
        Payment payment = findPayment(id);
        BigDecimal requestedAmount = normalizeRefundAmount(payment, request != null ? request.getAmount() : null);
        String reason = request != null ? trimToNull(request.getReason()) : null;

        if ("PAYPAL".equalsIgnoreCase(payment.getProvider())) {
            String captureId = firstNonBlank(payment.getProviderPaymentId(), fetchPayPalCaptureIdIfMissing(payment));
            PaymentMethod method = resolveMethodForPayment(payment);
            PayPalClient.PayPalRefundResult refund = payPalClient.refundCapture(
                resolvePayPalConfig(method),
                captureId,
                requestedAmount,
                payment.getCurrency(),
                reason,
                "refund-payment-" + payment.getId() + "-" + System.currentTimeMillis()
            );
            PayPalClient.PayPalOrderSnapshot snapshot = payPalClient.fetchOrderSnapshot(resolvePayPalConfig(method), payment.getProviderOrderId());
            return persistPayPalSnapshot(
                payment,
                snapshot,
                "PAYPAL_REFUND",
                "REFUND",
                serializePayload(Map.of(
                    "refund", refund,
                    "requestedAmount", requestedAmount,
                    "reason", reason == null ? "" : reason
                )),
                requestedAmount,
                true,
                false,
                false
            );
        }

        if ("FABRICK".equalsIgnoreCase(payment.getProvider())) {
            throw new BadRequestException("Automated Fabrick refunds are not implemented yet. Execute the refund in Fabrick and then run reconciliation.");
        }

        BigDecimal newRefundedAmount = defaultZero(payment.getRefundedAmount()).add(requestedAmount);
        updateRefundState(payment, newRefundedAmount);
        payment.setProviderStatus("MANUAL_REFUND");
        payment.setFailureCode(null);
        payment.setFailureReason(null);
        return persistVerifiedPayment(
            payment,
            "OFFLINE_REFUND",
            "REFUND",
            serializePayload(Map.of(
                "requestedAmount", requestedAmount,
                "reason", reason == null ? "" : reason
            )),
            requestedAmount,
            null,
            false,
            false,
            false
        );
    }

    @Transactional
    public PaymentResponse reconcile(Long id) {
        Payment payment = findPayment(id);
        if ("PAYPAL".equalsIgnoreCase(payment.getProvider())) {
            PaymentMethod method = resolveMethodForPayment(payment);
            PayPalClient.PayPalOrderSnapshot snapshot = payPalClient.fetchOrderSnapshot(resolvePayPalConfig(method), payment.getProviderOrderId());
            return persistPayPalSnapshot(
                payment,
                snapshot,
                "PAYPAL_RECONCILE",
                "RECONCILE",
                serializePayload(snapshot),
                payment.getAmount(),
                true,
                false,
                true
            );
        }
        if ("FABRICK".equalsIgnoreCase(payment.getProvider())) {
            PaymentMethod method = resolveMethodForPayment(payment);
            FabrickClient.FabrickPaymentDetail detail = fabrickClient.fetchPaymentDetail(
                resolveFabrickConfig(method),
                payment.getProviderPaymentId(),
                payment.getLightboxPaymentToken(),
                buildShopTransactionId(payment)
            );
            return persistFabrickState(
                payment,
                detail,
                "FABRICK_RECONCILE",
                "RECONCILE",
                serializePayload(detail),
                payment.getAmount(),
                true,
                false,
                true
            );
        }

        payment.setLastReconciledAt(OffsetDateTime.now());
        return persistVerifiedPayment(
            payment,
            "OFFLINE_RECONCILE",
            "RECONCILE",
            serializePayload(Map.of("status", payment.getStatus() == null ? "" : payment.getStatus())),
            payment.getAmount(),
            null,
            false,
            false,
            true
        );
    }

    @Transactional
    public PaymentResponse update(Long id, PaymentRequest request) {
        Payment payment = findPayment(id);

        applyUpdateRequest(payment, request);
        payment.setUpdatedAt(OffsetDateTime.now());

        Payment saved = paymentRepository.save(payment);
        recordPaymentTransaction(saved, "MANUAL_UPDATE", "PAYMENT_UPDATE", serializePayload(request), saved.getAmount(), firstNonBlank(saved.getProviderPaymentId(), saved.getProviderOrderId()));
        publishPaymentEvent("PAYMENT_UPDATED", saved);
        return toResponse(saved);
    }

    @Transactional
    public PaymentResponse upsertFromOrderEvent(Long orderId, BigDecimal amount, String currency) {
        Payment payment = paymentRepository.findFirstByOrderIdOrderByIdAsc(orderId)
            .orElseGet(Payment::new);

        boolean created = payment.getId() == null;
        OffsetDateTime now = OffsetDateTime.now();

        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setCurrency(normalizeCurrency(currency));
        payment.setRefundedAmount(defaultZero(payment.getRefundedAmount()));
        if (payment.getMethodCode() == null || payment.getMethodCode().isBlank()) {
            payment.setMethodCode("bank_transfer");
            payment.setMethodLabel("Bank transfer");
            payment.setProvider("OFFLINE");
        }

        if (created) {
            payment.setCreatedAt(now);
            if (payment.getStatus() == null) {
                payment.setStatus("CREATED");
            }
        }

        payment.setUpdatedAt(now);

        Payment saved = paymentRepository.save(payment);
        publishPaymentEvent(created ? "PAYMENT_CREATED" : "PAYMENT_UPDATED", saved);
        return toResponse(saved);
    }

    @Transactional
    public Map<String, Object> handlePayPalWebhook(
        String transmissionId,
        String transmissionTime,
        String certUrl,
        String authAlgo,
        String transmissionSig,
        String body
    ) {
        PaymentMethod method = paymentMethodRepository.findByCode("paypal").orElseGet(() -> fallbackMethodForProvider("PAYPAL"));
        if (!payPalClient.verifyWebhookSignature(resolvePayPalConfig(method), transmissionId, transmissionTime, certUrl, authAlgo, transmissionSig, body)) {
            throw new BadRequestException("Invalid PayPal webhook signature");
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            String eventType = trimToNull(root.path("event_type").asText(null));
            Payment payment = resolvePaymentFromPayPalWebhook(root);
            if (payment == null) {
                return webhookResponse("paypal", eventType, null, "ignored");
            }

            PaymentResponse updated;
            if ("PAYMENT.CAPTURE.REFUNDED".equalsIgnoreCase(eventType) && trimToNull(payment.getProviderOrderId()) != null) {
                PaymentMethod paymentMethod = resolveMethodForPayment(payment);
                PayPalClient.PayPalOrderSnapshot snapshot = payPalClient.fetchOrderSnapshot(resolvePayPalConfig(paymentMethod), payment.getProviderOrderId());
                updated = persistPayPalSnapshot(
                    payment,
                    snapshot,
                    "PAYPAL_WEBHOOK",
                    "WEBHOOK",
                    body,
                    payment.getAmount(),
                    true,
                    true,
                    false
                );
            } else {
                JsonNode resource = root.path("resource");
                String providerOrderId = firstNonBlank(
                    text(resource.path("supplementary_data").path("related_ids").path("order_id")),
                    eventType != null && eventType.startsWith("CHECKOUT.ORDER") ? text(resource.path("id")) : null,
                    payment.getProviderOrderId()
                );
                String providerPaymentId = firstNonBlank(
                    eventType != null && eventType.startsWith("PAYMENT.CAPTURE") ? text(resource.path("id")) : null,
                    text(resource.path("supplementary_data").path("related_ids").path("capture_id")),
                    payment.getProviderPaymentId()
                );
                String providerStatus = firstNonBlank(text(resource.path("status")), mapPayPalWebhookEventToStatus(eventType));

                updated = persistPayPalState(
                    payment,
                    providerOrderId,
                    providerPaymentId,
                    providerStatus,
                    "PAYPAL_WEBHOOK",
                    "WEBHOOK",
                    body,
                    payment.getAmount(),
                    true,
                    true,
                    false
                );
            }
            return webhookResponse("paypal", eventType, updated.getId(), updated.getStatus());
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Unable to process PayPal webhook: " + ex.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> handleFabrickWebhook(Map<String, String> parameters, String body) {
        Map<String, String> normalized = normalizeWebhookParameters(parameters, body);
        Payment payment = resolvePaymentFromFabrickWebhook(normalized);
        if (payment == null) {
            return webhookResponse("fabrick", normalized.getOrDefault("status", "UNKNOWN"), null, "ignored");
        }

        FabrickClient.FabrickPaymentDetail detail = fabrickClient.fetchPaymentDetail(
            resolveFabrickConfig(resolveMethodForPayment(payment)),
            firstNonBlank(normalized.get("providerPaymentId"), payment.getProviderPaymentId()),
            firstNonBlank(normalized.get("paymentToken"), payment.getLightboxPaymentToken()),
            firstNonBlank(normalized.get("shopTransactionId"), buildShopTransactionId(payment))
        );
        PaymentResponse updated = persistFabrickState(
            payment,
            detail,
            "FABRICK_WEBHOOK",
            "WEBHOOK",
            body != null && !body.isBlank() ? body : serializePayload(normalized),
            payment.getAmount(),
            true,
            true,
            false
        );
        return webhookResponse("fabrick", detail.transactionResult(), updated.getId(), updated.getStatus());
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(Long id) {
        return toResponse(findPayment(id));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> list(Long orderId) {
        if (orderId != null) {
            return paymentRepository.findByOrderId(orderId).stream().map(this::toResponse).collect(Collectors.toList());
        }
        return paymentRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        Payment payment = findPayment(id);
        paymentRepository.delete(payment);
        eventPublisher.publish("PAYMENT_DELETED", "payment", id.toString(), null);
    }

    private void initiatePayment(Payment payment, PaymentRequest request, PaymentMethod method) {
        String provider = method.getProvider().toUpperCase(Locale.ROOT);
        payment.setProviderEnvironment(resolveEnvironment(method));
        if ("OFFLINE".equals(provider)) {
            payment.setStatus(request.getStatus() != null ? request.getStatus() : "PENDING_OFFLINE");
            return;
        }
        if ("PAYPAL".equals(provider)) {
            if (!payPalClient.isAvailable(resolvePayPalConfig(method))) {
                throw new BadRequestException("PayPal is not available. Configure sandbox or production credentials first.");
            }
            PaymentRequest providerRequest = copyRequestWithUrls(request, payment);
            PayPalClient.PayPalCreateResult result = payPalClient.createOrder(resolvePayPalConfig(method), payment, providerRequest);
            payment.setProviderOrderId(result.orderId());
            payment.setProviderStatus(result.status());
            payment.setApprovalUrl(result.approvalUrl());
            payment.setRedirectUrl(result.approvalUrl());
            payment.setStatus("REDIRECT_REQUIRED");
            return;
        }
        if ("FABRICK".equals(provider)) {
            if (!fabrickClient.isAvailable(resolveFabrickConfig(method))) {
                throw new BadRequestException("Fabrick is not available. Configure sandbox or production credentials first.");
            }
            FabrickClient.FabrickCreateResult result = fabrickClient.createHostedPayment(resolveFabrickConfig(method), payment);
            payment.setProviderPaymentId(result.paymentId());
            payment.setLightboxPaymentToken(result.paymentToken());
            payment.setLightboxScriptUrl(result.scriptUrl());
            payment.setLightboxShopLogin(result.shopLogin());
            payment.setRedirectUrl(result.redirectUrl());
            payment.setStatus("REDIRECT_REQUIRED");
            return;
        }
        throw new BadRequestException("Unsupported payment provider: " + provider);
    }

    private PaymentRequest copyRequestWithUrls(PaymentRequest request, Payment payment) {
        PaymentRequest copy = new PaymentRequest();
        copy.setOrderId(request.getOrderId());
        copy.setAmount(request.getAmount());
        copy.setCurrency(request.getCurrency());
        copy.setStatus(request.getStatus());
        copy.setProvider(request.getProvider());
        copy.setMethodCode(request.getMethodCode());
        copy.setReturnUrl(payment.getReturnUrl());
        copy.setCancelUrl(payment.getCancelUrl());
        copy.setPayerEmail(request.getPayerEmail());
        copy.setPayerName(request.getPayerName());
        copy.setIdempotencyKey(request.getIdempotencyKey());
        return copy;
    }

    private void applyUpdateRequest(Payment payment, PaymentRequest request) {
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(normalizeCurrency(request.getCurrency()));
        if (request.getStatus() != null) {
            payment.setStatus(request.getStatus());
        }
        if (request.getProvider() != null) {
            payment.setProvider(request.getProvider());
        }
        if (request.getMethodCode() != null) {
            payment.setMethodCode(request.getMethodCode());
        }
        if (request.getReturnUrl() != null) {
            payment.setReturnUrl(request.getReturnUrl());
        }
        if (request.getCancelUrl() != null) {
            payment.setCancelUrl(request.getCancelUrl());
        }
    }

    private void applyMethodRequest(PaymentMethod method, PaymentMethodRequest request) {
        String provider = requiredTrimmed(request.getProvider(), "provider").toUpperCase(Locale.ROOT);
        method.setCode(normalizeMethodCode(trimToNull(request.getCode()), null));
        method.setDisplayName(requiredTrimmed(request.getDisplayName(), "displayName"));
        method.setProvider(provider);
        method.setPaymentFlow(requiredTrimmed(request.getPaymentFlow(), "paymentFlow").toUpperCase(Locale.ROOT));
        method.setDescription(trimToNull(request.getDescription()));
        method.setActive(request.getActive() == null || request.getActive());
        method.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        method.setProviderEnvironment(trimToNull(request.getProviderEnvironment()));
        method.setProviderBaseUrl(trimToNull(request.getProviderBaseUrl()));

        if ("PAYPAL".equals(provider)) {
            method.setProviderBrandName(trimToNull(request.getProviderBrandName()));
            method.setProviderWebhookId(trimToNull(request.getProviderWebhookId()));
            method.setProviderClientId(trimToNull(request.getProviderClientId()));
            if (Boolean.TRUE.equals(request.getClearProviderClientSecret())) {
                method.setProviderClientSecretEncrypted(null);
            } else if (trimToNull(request.getProviderClientSecret()) != null) {
                method.setProviderClientSecretEncrypted(paymentCredentialCryptoService.encrypt(trimToNull(request.getProviderClientSecret())));
            }
        } else {
            method.setProviderBrandName(null);
            method.setProviderWebhookId(null);
            method.setProviderClientId(null);
            method.setProviderClientSecretEncrypted(null);
        }

        if ("FABRICK".equals(provider)) {
            method.setProviderShopLogin(trimToNull(request.getProviderShopLogin()));
            method.setProviderLightboxScriptUrl(trimToNull(request.getProviderLightboxScriptUrl()));
            method.setProviderNotificationUrl(trimToNull(request.getProviderNotificationUrl()));
            if (Boolean.TRUE.equals(request.getClearProviderApiKey())) {
                method.setProviderApiKeyEncrypted(null);
            } else if (trimToNull(request.getProviderApiKey()) != null) {
                method.setProviderApiKeyEncrypted(paymentCredentialCryptoService.encrypt(trimToNull(request.getProviderApiKey())));
            }
        } else {
            method.setProviderShopLogin(null);
            method.setProviderApiKeyEncrypted(null);
            method.setProviderLightboxScriptUrl(null);
            method.setProviderNotificationUrl(null);
        }
    }

    private PaymentMethod resolveMethod(String methodCode) {
        return paymentMethodRepository.findByCode(methodCode)
            .filter(PaymentMethod::getActive)
            .orElseThrow(() -> new BadRequestException("Payment method not available: " + methodCode));
    }

    private PaymentMethod findMethodById(Long id) {
        return paymentMethodRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Payment method not found"));
    }

    private Payment findPayment(Long id) {
        return paymentRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Payment not found"));
    }

    private boolean isMethodAvailable(PaymentMethod method) {
        return switch (method.getProvider().toUpperCase(Locale.ROOT)) {
            case "OFFLINE" -> true;
            case "PAYPAL" -> payPalClient.isAvailable(resolvePayPalConfig(method));
            case "FABRICK" -> fabrickClient.isAvailable(resolveFabrickConfig(method));
            default -> false;
        };
    }

    private String resolveEnvironment(PaymentMethod method) {
        String provider = method.getProvider().toUpperCase(Locale.ROOT);
        return switch (provider) {
            case "PAYPAL" -> firstNonBlank(resolvePayPalConfig(method).environment(), "production");
            case "FABRICK" -> firstNonBlank(resolveFabrickConfig(method).environment(), "production");
            default -> "internal";
        };
    }

    private PaymentResponse persistPayPalState(
        Payment payment,
        String providerOrderId,
        String providerPaymentId,
        String providerStatus,
        String source,
        String operationType,
        String rawPayload,
        BigDecimal transactionAmount,
        boolean providerSync,
        boolean webhook,
        boolean reconciled
    ) {
        payment.setProviderOrderId(firstNonBlank(providerOrderId, payment.getProviderOrderId()));
        payment.setProviderPaymentId(firstNonBlank(providerPaymentId, payment.getProviderPaymentId()));
        payment.setProviderStatus(providerStatus);
        payment.setRefundedAmount(defaultZero(payment.getRefundedAmount()));
        payment.setStatus(mapPayPalStatus(providerStatus));
        synchronizeFailureState(payment, "PayPal returned status " + providerStatus);
        return persistVerifiedPayment(
            payment,
            source,
            operationType,
            rawPayload,
            transactionAmount,
            firstNonBlank(providerPaymentId, providerOrderId),
            providerSync,
            webhook,
            reconciled
        );
    }

    private PaymentResponse persistPayPalSnapshot(
        Payment payment,
        PayPalClient.PayPalOrderSnapshot snapshot,
        String source,
        String operationType,
        String rawPayload,
        BigDecimal transactionAmount,
        boolean providerSync,
        boolean webhook,
        boolean reconciled
    ) {
        payment.setProviderOrderId(firstNonBlank(snapshot.orderId(), payment.getProviderOrderId()));
        payment.setProviderPaymentId(firstNonBlank(snapshot.captureId(), payment.getProviderPaymentId()));
        payment.setProviderStatus(firstNonBlank(snapshot.captureStatus(), snapshot.orderStatus(), payment.getProviderStatus()));

        BigDecimal refundedAmount = defaultZero(snapshot.refundedAmount());
        if (refundedAmount.signum() > 0) {
            updateRefundState(payment, refundedAmount);
        } else {
            payment.setRefundedAmount(BigDecimal.ZERO);
            payment.setStatus(mapPayPalStatus(payment.getProviderStatus()));
        }
        synchronizeFailureState(payment, "PayPal returned status " + payment.getProviderStatus());
        return persistVerifiedPayment(
            payment,
            source,
            operationType,
            rawPayload,
            transactionAmount,
            firstNonBlank(snapshot.captureId(), snapshot.orderId()),
            providerSync,
            webhook,
            reconciled
        );
    }

    private PaymentResponse persistFabrickState(
        Payment payment,
        FabrickClient.FabrickPaymentDetail detail,
        String source,
        String operationType,
        String rawPayload,
        BigDecimal transactionAmount,
        boolean providerSync,
        boolean webhook,
        boolean reconciled
    ) {
        payment.setProviderPaymentId(firstNonBlank(detail.paymentId(), payment.getProviderPaymentId()));
        payment.setProviderStatus(firstNonBlank(detail.transactionResult(), detail.transactionState()));
        payment.setRefundedAmount(defaultZero(payment.getRefundedAmount()));
        payment.setStatus(applyRefundAwareStatus(mapFabrickStatus(detail.transactionResult(), detail.transactionState()), payment.getRefundedAmount(), payment.getAmount()));
        if ("FAILED".equalsIgnoreCase(payment.getStatus()) || "CANCELLED".equalsIgnoreCase(payment.getStatus())) {
            payment.setFailureCode(trimToNull(detail.errorCode()));
            payment.setFailureReason(firstNonBlank(trimToNull(detail.errorDescription()), "Fabrick transaction result " + firstNonBlank(detail.transactionResult(), detail.transactionState())));
        } else {
            payment.setFailureCode(null);
            payment.setFailureReason(null);
        }
        return persistVerifiedPayment(
            payment,
            source,
            operationType,
            rawPayload,
            transactionAmount,
            firstNonBlank(detail.paymentId(), detail.shopTransactionId()),
            providerSync,
            webhook,
            reconciled
        );
    }

    private PaymentResponse persistVerifiedPayment(
        Payment payment,
        String source,
        String operationType,
        String rawPayload,
        BigDecimal transactionAmount,
        String providerReference,
        boolean providerSync,
        boolean webhook,
        boolean reconciled
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        payment.setUpdatedAt(now);
        if (providerSync) {
            payment.setLastProviderSyncAt(now);
        }
        if (webhook) {
            payment.setLastWebhookAt(now);
        }
        if (reconciled) {
            payment.setLastReconciledAt(now);
        }
        Payment saved = paymentRepository.save(payment);
        recordPaymentTransaction(saved, operationType, source, rawPayload, transactionAmount, providerReference);
        publishPaymentEvent("PAYMENT_UPDATED", saved);
        return toResponse(saved);
    }

    private void recordPaymentTransaction(Payment payment, String operationType, String eventSource, String rawPayload, BigDecimal amount, String providerReference) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setPayment(payment);
        transaction.setOrderId(payment.getOrderId());
        transaction.setOperationType(operationType);
        transaction.setEventSource(eventSource);
        transaction.setStatus(payment.getStatus());
        transaction.setProviderStatus(payment.getProviderStatus());
        transaction.setProviderReference(providerReference);
        transaction.setAmount(amount);
        transaction.setCurrency(payment.getCurrency());
        transaction.setFailureCode(payment.getFailureCode());
        transaction.setFailureReason(payment.getFailureReason());
        transaction.setRawPayload(trimToNull(rawPayload));
        OffsetDateTime now = OffsetDateTime.now();
        transaction.setCreatedAt(now);
        transaction.setUpdatedAt(now);
        paymentTransactionRepository.save(transaction);
    }

    private void updateRefundState(Payment payment, BigDecimal refundedAmount) {
        BigDecimal normalized = clamp(defaultZero(refundedAmount), BigDecimal.ZERO, defaultZero(payment.getAmount()));
        payment.setRefundedAmount(normalized);
        if (normalized.signum() <= 0) {
            return;
        }
        if (defaultZero(payment.getAmount()).compareTo(normalized) <= 0) {
            payment.setStatus("REFUNDED");
        } else {
            payment.setStatus("PARTIALLY_REFUNDED");
        }
        payment.setFailureCode(null);
        payment.setFailureReason(null);
    }

    private void synchronizeFailureState(Payment payment, String fallbackReason) {
        if ("FAILED".equalsIgnoreCase(payment.getStatus()) || "CANCELLED".equalsIgnoreCase(payment.getStatus())) {
            if (payment.getFailureReason() == null || payment.getFailureReason().isBlank()) {
                payment.setFailureReason(trimToNull(fallbackReason));
            }
        } else {
            payment.setFailureCode(null);
            payment.setFailureReason(null);
        }
    }

    private BigDecimal normalizeRefundAmount(Payment payment, BigDecimal requestedAmount) {
        BigDecimal alreadyRefunded = defaultZero(payment.getRefundedAmount());
        BigDecimal maxRefundable = defaultZero(payment.getAmount()).subtract(alreadyRefunded);
        if (maxRefundable.signum() <= 0) {
            throw new BadRequestException("Payment is already fully refunded");
        }
        BigDecimal normalized = requestedAmount == null ? maxRefundable : requestedAmount;
        if (normalized.signum() <= 0) {
            throw new BadRequestException("Refund amount must be greater than zero");
        }
        if (normalized.compareTo(maxRefundable) > 0) {
            throw new BadRequestException("Refund amount exceeds the remaining refundable total");
        }
        return normalized;
    }

    private String fetchPayPalCaptureIdIfMissing(Payment payment) {
        if (trimToNull(payment.getProviderPaymentId()) != null) {
            return payment.getProviderPaymentId();
        }
        if (trimToNull(payment.getProviderOrderId()) == null) {
            throw new BadRequestException("PayPal capture id is missing and the payment has no provider order id to reconcile");
        }
        PayPalClient.PayPalOrderSnapshot snapshot = payPalClient.fetchOrderSnapshot(resolvePayPalConfig(resolveMethodForPayment(payment)), payment.getProviderOrderId());
        payment.setProviderOrderId(firstNonBlank(snapshot.orderId(), payment.getProviderOrderId()));
        payment.setProviderPaymentId(firstNonBlank(snapshot.captureId(), payment.getProviderPaymentId()));
        if (trimToNull(payment.getProviderPaymentId()) == null) {
            throw new BadRequestException("Unable to resolve PayPal capture id from provider order " + payment.getProviderOrderId());
        }
        return payment.getProviderPaymentId();
    }

    private Payment resolvePaymentFromPayPalWebhook(JsonNode root) {
        JsonNode resource = root.path("resource");
        Long internalPaymentId = parseInternalPaymentId(firstNonBlank(
            text(resource.path("custom_id")),
            findPurchaseUnitField(resource, "custom_id")
        ));
        if (internalPaymentId != null) {
            return paymentRepository.findById(internalPaymentId).orElse(null);
        }

        String providerPaymentId = firstNonBlank(text(resource.path("id")), text(resource.path("supplementary_data").path("related_ids").path("capture_id")));
        if (providerPaymentId != null) {
            Optional<Payment> byCaptureId = paymentRepository.findFirstByProviderPaymentId(providerPaymentId);
            if (byCaptureId.isPresent()) {
                return byCaptureId.get();
            }
        }

        String providerOrderId = firstNonBlank(
            text(resource.path("supplementary_data").path("related_ids").path("order_id")),
            text(resource.path("id")),
            findPurchaseUnitField(resource, "reference_id")
        );
        if (providerOrderId != null) {
            String normalizedOrderId = providerOrderId.startsWith("ORDER-") ? providerOrderId.substring("ORDER-".length()) : providerOrderId;
            Optional<Payment> byProviderOrder = paymentRepository.findFirstByProviderOrderId(providerOrderId);
            if (byProviderOrder.isPresent()) {
                return byProviderOrder.get();
            }
            Long orderId = parseLong(normalizedOrderId);
            if (orderId != null) {
                return paymentRepository.findFirstByOrderIdOrderByIdAsc(orderId).orElse(null);
            }
        }
        return null;
    }

    private Payment resolvePaymentFromFabrickWebhook(Map<String, String> parameters) {
        Long internalPaymentId = parseLong(firstNonBlank(parameters.get("paymentId"), parameters.get("payment_id")));
        if (internalPaymentId != null) {
            return paymentRepository.findById(internalPaymentId).orElse(null);
        }

        String providerPaymentId = firstNonBlank(parameters.get("providerPaymentId"), parameters.get("paymentID"), parameters.get("paymentid"));
        if (providerPaymentId != null) {
            Optional<Payment> byProviderPaymentId = paymentRepository.findFirstByProviderPaymentId(providerPaymentId);
            if (byProviderPaymentId.isPresent()) {
                return byProviderPaymentId.get();
            }
        }

        String paymentToken = firstNonBlank(parameters.get("paymentToken"), parameters.get("paymenttoken"));
        if (paymentToken != null) {
            Optional<Payment> byToken = paymentRepository.findFirstByLightboxPaymentToken(paymentToken);
            if (byToken.isPresent()) {
                return byToken.get();
            }
        }

        Long orderId = parseLong(firstNonBlank(parameters.get("orderId"), parameters.get("order_id")));
        if (orderId != null) {
            Optional<Payment> byOrderId = paymentRepository.findFirstByOrderIdOrderByIdAsc(orderId);
            if (byOrderId.isPresent()) {
                return byOrderId.get();
            }
        }

        String shopTransactionId = firstNonBlank(parameters.get("shopTransactionID"), parameters.get("shoptransactionid"));
        Long parsedPaymentId = parseInternalPaymentIdFromShopTransaction(shopTransactionId);
        if (parsedPaymentId != null) {
            return paymentRepository.findById(parsedPaymentId).orElse(null);
        }
        return null;
    }

    private Map<String, String> normalizeWebhookParameters(Map<String, String> parameters, String body) {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (parameters != null) {
            normalized.putAll(parameters);
        }
        if (body != null && !body.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(body);
                copyIfPresent(normalized, "providerPaymentId", root, "providerPaymentId");
                copyIfPresent(normalized, "providerPaymentId", root, "paymentID");
                copyIfPresent(normalized, "paymentToken", root, "paymentToken");
                copyIfPresent(normalized, "paymentId", root, "paymentId");
                copyIfPresent(normalized, "orderId", root, "orderId");
                copyIfPresent(normalized, "shopTransactionId", root, "shopTransactionID");
                copyIfPresent(normalized, "status", root, "status");
            } catch (Exception ignored) {
                // best effort only: Fabrick can call back as query params or form body.
            }
        }
        return normalized;
    }

    private void copyIfPresent(Map<String, String> target, String key, JsonNode source, String sourceKey) {
        String value = text(source.path(sourceKey));
        if (value != null && !target.containsKey(key)) {
            target.put(key, value);
        }
    }

    private Map<String, Object> webhookResponse(String provider, String event, Long paymentId, String status) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("provider", provider);
        response.put("event", event);
        response.put("paymentId", paymentId);
        response.put("status", status);
        return response;
    }

    private void publishPaymentEvent(String eventType, Payment payment) {
        eventPublisher.publish(eventType, "payment", payment.getId().toString(), toResponse(payment));
    }

    private AdminPaymentMethodResponse toAdminMethodResponse(PaymentMethod method) {
        AdminPaymentMethodResponse response = new AdminPaymentMethodResponse();
        response.setId(method.getId());
        response.setCode(method.getCode());
        response.setDisplayName(method.getDisplayName());
        response.setProvider(method.getProvider());
        response.setPaymentFlow(method.getPaymentFlow());
        response.setDescription(method.getDescription());
        response.setActive(method.getActive());
        response.setSortOrder(method.getSortOrder());
        response.setProviderConfigurationAvailable(true);

        String provider = method.getProvider() == null ? "" : method.getProvider().toUpperCase(Locale.ROOT);
        if ("PAYPAL".equals(provider)) {
            PaymentMethodProviderConfigurationResolver.ResolvedPayPalConfig config = resolvePayPalConfig(method);
            response.setProviderEnvironment(config.environment());
            response.setProviderBaseUrl(config.baseUrl());
            response.setProviderBrandName(config.brandName());
            response.setProviderWebhookId(config.webhookId());
            response.setProviderClientId(config.clientId());
            response.setProviderClientSecretConfigured(!"missing".equals(config.clientSecretSource()));
            response.setProviderClientSecretSource(config.clientSecretSource());
            response.setProviderConfigurationAvailable(config.isAvailable());
        } else if ("FABRICK".equals(provider)) {
            PaymentMethodProviderConfigurationResolver.ResolvedFabrickConfig config = resolveFabrickConfig(method);
            response.setProviderEnvironment(config.environment());
            response.setProviderBaseUrl(config.baseUrl());
            response.setProviderShopLogin(config.shopLogin());
            response.setProviderApiKeyConfigured(!"missing".equals(config.apiKeySource()));
            response.setProviderApiKeySource(config.apiKeySource());
            response.setProviderLightboxScriptUrl(config.lightboxScriptUrl());
            response.setProviderNotificationUrl(config.notificationUrl());
            response.setProviderConfigurationAvailable(config.isAvailable());
        }
        return response;
    }

    private PaymentMethodResponse toMethodResponse(PaymentMethod method) {
        PaymentMethodResponse response = new PaymentMethodResponse();
        response.setId(method.getId());
        response.setCode(method.getCode());
        response.setDisplayName(method.getDisplayName());
        response.setProvider(method.getProvider());
        response.setPaymentFlow(method.getPaymentFlow());
        response.setDescription(method.getDescription());
        response.setActive(method.getActive());
        response.setSortOrder(method.getSortOrder());
        return response;
    }

    private PaymentMethodProviderConfigurationResolver.ResolvedPayPalConfig resolvePayPalConfig(PaymentMethod method) {
        return providerConfigurationResolver.resolvePayPal(method);
    }

    private PaymentMethodProviderConfigurationResolver.ResolvedFabrickConfig resolveFabrickConfig(PaymentMethod method) {
        return providerConfigurationResolver.resolveFabrick(method);
    }

    private PaymentMethod resolveMethodForPayment(Payment payment) {
        String methodCode = trimToNull(payment.getMethodCode());
        if (methodCode != null) {
            return paymentMethodRepository.findByCode(methodCode).orElseGet(() -> fallbackMethodForProvider(payment.getProvider()));
        }
        return fallbackMethodForProvider(payment.getProvider());
    }

    private PaymentMethod fallbackMethodForProvider(String provider) {
        String normalizedProvider = provider == null ? "OFFLINE" : provider.trim().toUpperCase(Locale.ROOT);
        PaymentMethod method = new PaymentMethod();
        method.setProvider(normalizedProvider);
        method.setCode(normalizeMethodCode(null, normalizedProvider));
        method.setDisplayName(normalizedProvider);
        method.setPaymentFlow("OFFLINE");
        method.setActive(true);
        method.setSortOrder(0);
        return method;
    }

    private PaymentResponse toResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setOrderId(payment.getOrderId());
        response.setAmount(payment.getAmount());
        response.setRefundedAmount(defaultZero(payment.getRefundedAmount()));
        response.setCurrency(payment.getCurrency());
        response.setStatus(payment.getStatus());
        response.setProvider(payment.getProvider());
        response.setMethodCode(payment.getMethodCode());
        response.setMethodLabel(payment.getMethodLabel());
        response.setProviderOrderId(payment.getProviderOrderId());
        response.setProviderPaymentId(payment.getProviderPaymentId());
        response.setProviderEnvironment(payment.getProviderEnvironment());
        response.setProviderStatus(payment.getProviderStatus());
        response.setRedirectUrl(payment.getRedirectUrl());
        response.setApprovalUrl(payment.getApprovalUrl());
        response.setReturnUrl(payment.getReturnUrl());
        response.setCancelUrl(payment.getCancelUrl());
        response.setFailureCode(payment.getFailureCode());
        response.setFailureReason(payment.getFailureReason());
        response.setLightboxScriptUrl(payment.getLightboxScriptUrl());
        response.setLightboxShopLogin(payment.getLightboxShopLogin());
        response.setLightboxPaymentToken(payment.getLightboxPaymentToken());
        response.setLastReconciledAt(payment.getLastReconciledAt());
        response.setLastWebhookAt(payment.getLastWebhookAt());
        response.setLastProviderSyncAt(payment.getLastProviderSyncAt());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        return response;
    }

    private String normalizeMethodCode(String methodCode, String provider) {
        String raw = trimToNull(methodCode) != null ? trimToNull(methodCode) : trimToNull(provider);
        if (raw == null) {
            return "bank_transfer";
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        return switch (normalized) {
            case "cod" -> "cash_on_delivery";
            case "bank", "bank_transfer" -> "bank_transfer";
            case "paypal" -> "paypal";
            case "fabrick", "sella", "banca_sella", "fabrick_gateway" -> "fabrick_gateway";
            default -> normalized;
        };
    }

    private String normalizeCurrency(String currency) {
        String normalized = trimToNull(currency);
        return normalized == null ? "EUR" : normalized.toUpperCase(Locale.ROOT);
    }

    private String appendQueryParamIfMissing(String url, String key, String value) {
        if (trimToNull(url) == null || url.contains(key + "=")) {
            return url;
        }
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + key + '=' + value;
    }

    private String mapPayPalStatus(String status) {
        if (status == null) {
            return "FAILED";
        }
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "COMPLETED", "CAPTURED", "SETTLED" -> "CAPTURED";
            case "AUTHORIZED" -> "AUTHORIZED";
            case "APPROVED", "PAYER_ACTION_REQUIRED" -> "APPROVED";
            case "PENDING" -> "CAPTURE_PENDING";
            case "VOIDED", "CANCELLED" -> "CANCELLED";
            case "REFUNDED" -> "REFUNDED";
            default -> "FAILED";
        };
    }

    private String mapPayPalWebhookEventToStatus(String eventType) {
        if (eventType == null) {
            return null;
        }
        return switch (eventType.toUpperCase(Locale.ROOT)) {
            case "CHECKOUT.ORDER.APPROVED" -> "APPROVED";
            case "PAYMENT.CAPTURE.COMPLETED" -> "COMPLETED";
            case "PAYMENT.CAPTURE.PENDING" -> "PENDING";
            case "PAYMENT.CAPTURE.DENIED", "PAYMENT.CAPTURE.DECLINED" -> "FAILED";
            case "PAYMENT.CAPTURE.REFUNDED" -> "REFUNDED";
            case "CHECKOUT.ORDER.VOIDED" -> "CANCELLED";
            default -> null;
        };
    }

    private String mapFabrickStatus(String transactionResult, String transactionState) {
        String result = trimToNull(transactionResult);
        String state = trimToNull(transactionState);
        if (result != null && "KO".equalsIgnoreCase(result)) {
            return "FAILED";
        }
        if (state == null) {
            return result != null && "OK".equalsIgnoreCase(result) ? "CAPTURED" : "PENDING_PAYMENT";
        }
        return switch (state.toUpperCase(Locale.ROOT)) {
            case "MOV", "STO" -> "CAPTURED";
            case "AUT" -> "AUTHORIZED";
            case "CAN" -> "CANCELLED";
            default -> result != null && "OK".equalsIgnoreCase(result) ? "CAPTURED" : "PENDING_PAYMENT";
        };
    }

    private String applyRefundAwareStatus(String baseStatus, BigDecimal refundedAmount, BigDecimal totalAmount) {
        BigDecimal normalizedRefunded = defaultZero(refundedAmount);
        if (normalizedRefunded.signum() <= 0) {
            return baseStatus;
        }
        return defaultZero(totalAmount).compareTo(normalizedRefunded) <= 0 ? "REFUNDED" : "PARTIALLY_REFUNDED";
    }

    private void ensureProvider(Payment payment, String provider) {
        if (payment.getProvider() == null || !provider.equalsIgnoreCase(payment.getProvider())) {
            throw new BadRequestException("Payment does not belong to provider " + provider);
        }
    }

    private String buildShopTransactionId(Payment payment) {
        return "ORDER-" + payment.getOrderId() + "-PAY-" + payment.getId();
    }

    private String findPurchaseUnitField(JsonNode resource, String fieldName) {
        JsonNode purchaseUnits = resource.path("purchase_units");
        if (!purchaseUnits.isArray()) {
            return null;
        }
        for (JsonNode purchaseUnit : purchaseUnits) {
            String value = text(purchaseUnit.path(fieldName));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Long parseInternalPaymentId(String customId) {
        if (customId == null) {
            return null;
        }
        if (customId.startsWith("PAYMENT-")) {
            return parseLong(customId.substring("PAYMENT-".length()));
        }
        return parseLong(customId);
    }

    private Long parseInternalPaymentIdFromShopTransaction(String shopTransactionId) {
        if (shopTransactionId == null) {
            return null;
        }
        int marker = shopTransactionId.lastIndexOf("-PAY-");
        if (marker >= 0) {
            return parseLong(shopTransactionId.substring(marker + 5));
        }
        return null;
    }

    private Long parseLong(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText(null);
        return trimToNull(text);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        BigDecimal normalized = defaultZero(value);
        if (normalized.compareTo(min) < 0) {
            return min;
        }
        if (normalized.compareTo(max) > 0) {
            return max;
        }
        return normalized;
    }

    private String requiredTrimmed(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BadRequestException("Missing required field: " + fieldName);
        }
        return normalized;
    }

    private String serializePayload(Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof String text) {
            return trimToNull(text);
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return String.valueOf(payload);
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
