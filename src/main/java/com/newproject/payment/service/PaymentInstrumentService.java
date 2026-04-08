package com.newproject.payment.service;

import com.newproject.payment.domain.PaymentInstrument;
import com.newproject.payment.domain.PaymentMethod;
import com.newproject.payment.dto.PaymentInstrumentRequest;
import com.newproject.payment.dto.PaymentInstrumentResponse;
import com.newproject.payment.exception.BadRequestException;
import com.newproject.payment.exception.NotFoundException;
import com.newproject.payment.repository.PaymentInstrumentRepository;
import com.newproject.payment.repository.PaymentMethodRepository;
import com.newproject.payment.security.RequestActor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentInstrumentService {
    private final PaymentInstrumentRepository paymentInstrumentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentCredentialCryptoService paymentCredentialCryptoService;
    private final PaymentMethodProviderConfigurationResolver providerConfigurationResolver;
    private final RequestActor requestActor;
    private final PayPalClient payPalClient;

    public PaymentInstrumentService(
        PaymentInstrumentRepository paymentInstrumentRepository,
        PaymentMethodRepository paymentMethodRepository,
        PaymentCredentialCryptoService paymentCredentialCryptoService,
        PaymentMethodProviderConfigurationResolver providerConfigurationResolver,
        RequestActor requestActor,
        PayPalClient payPalClient
    ) {
        this.paymentInstrumentRepository = paymentInstrumentRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentCredentialCryptoService = paymentCredentialCryptoService;
        this.providerConfigurationResolver = providerConfigurationResolver;
        this.requestActor = requestActor;
        this.payPalClient = payPalClient;
    }

    @Transactional(readOnly = true)
    public List<PaymentInstrumentResponse> listForCustomer(Long customerId) {
        Long scopedCustomerId = requestActor.resolveScopedCustomerId(customerId);
        return paymentInstrumentRepository.findByCustomerIdOrderByDefaultInstrumentDescCreatedAtDesc(scopedCustomerId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public PaymentInstrumentResponse create(Long customerId, PaymentInstrumentRequest request) {
        Long scopedCustomerId = requestActor.resolveScopedCustomerId(customerId);
        PaymentMethod method = resolveTokenizableMethod(request.getPaymentMethodCode());
        ResolvedProviderToken resolvedProviderToken = resolveProviderToken(method, request);
        String providerToken = requiredTrimmed(resolvedProviderToken.providerToken(), "providerToken");
        String fingerprint = fingerprint(providerToken);

        PaymentInstrument instrument = paymentInstrumentRepository.findFirstByCustomerIdAndProviderTokenFingerprint(scopedCustomerId, fingerprint)
            .orElseGet(PaymentInstrument::new);
        boolean created = instrument.getId() == null;
        OffsetDateTime now = OffsetDateTime.now();
        if (created) {
            instrument.setCustomerId(scopedCustomerId);
            instrument.setCreatedAt(now);
        }

        applyRequest(instrument, request, method, providerToken, fingerprint, resolvedProviderToken);
        instrument.setUpdatedAt(now);
        PaymentInstrument saved = paymentInstrumentRepository.save(instrument);
        synchronizeDefaults(saved);
        return toResponse(saved);
    }

    @Transactional
    public PaymentInstrumentResponse update(Long customerId, Long instrumentId, PaymentInstrumentRequest request) {
        Long scopedCustomerId = requestActor.resolveScopedCustomerId(customerId);
        PaymentInstrument instrument = paymentInstrumentRepository.findByIdAndCustomerId(instrumentId, scopedCustomerId)
            .orElseThrow(() -> new NotFoundException("Payment instrument not found"));

        PaymentMethod method = resolveTokenizableMethod(firstNonBlank(trimToNull(request.getPaymentMethodCode()), instrument.getPaymentMethodCode()));
        ResolvedProviderToken resolvedProviderToken = resolveProviderToken(method, request);
        String providerToken = trimToNull(resolvedProviderToken.providerToken());
        String fingerprint = providerToken != null ? fingerprint(providerToken) : instrument.getProviderTokenFingerprint();

        applyRequest(instrument, request, method, providerToken, fingerprint, resolvedProviderToken);
        instrument.setUpdatedAt(OffsetDateTime.now());
        PaymentInstrument saved = paymentInstrumentRepository.save(instrument);
        synchronizeDefaults(saved);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long customerId, Long instrumentId) {
        Long scopedCustomerId = requestActor.resolveScopedCustomerId(customerId);
        PaymentInstrument instrument = paymentInstrumentRepository.findByIdAndCustomerId(instrumentId, scopedCustomerId)
            .orElseThrow(() -> new NotFoundException("Payment instrument not found"));
        paymentInstrumentRepository.delete(instrument);
    }

    private void applyRequest(
        PaymentInstrument instrument,
        PaymentInstrumentRequest request,
        PaymentMethod method,
        String providerToken,
        String fingerprint,
        ResolvedProviderToken resolvedProviderToken
    ) {
        instrument.setPaymentMethodCode(method.getCode());
        instrument.setProvider(method.getProvider().toUpperCase(Locale.ROOT));
        instrument.setDisplayLabel(firstNonBlank(
            trimToNull(request.getDisplayLabel()),
            resolvedProviderToken.displayLabel(),
            method.getDisplayName()
        ));
        instrument.setBrand(firstNonBlank(trimToNull(request.getBrand()), resolvedProviderToken.brand()));
        instrument.setLast4(normalizeLast4(request.getLast4()));
        instrument.setExpiryMonth(normalizeExpiryMonth(request.getExpiryMonth()));
        instrument.setExpiryYear(normalizeExpiryYear(request.getExpiryYear()));
        instrument.setGatewayCustomerReference(firstNonBlank(
            trimToNull(request.getGatewayCustomerReference()),
            resolvedProviderToken.gatewayCustomerReference()
        ));
        instrument.setActive(request.getActive() == null || request.getActive());
        instrument.setDefaultInstrument(Boolean.TRUE.equals(request.getDefaultInstrument()));
        if (providerToken != null) {
            instrument.setProviderTokenEncrypted(paymentCredentialCryptoService.encrypt(providerToken));
            instrument.setProviderTokenFingerprint(fingerprint);
        }
        if (trimToNull(instrument.getProviderTokenEncrypted()) == null) {
            throw new BadRequestException("Provider token is required");
        }
        if (Boolean.TRUE.equals(instrument.getDefaultInstrument())) {
            instrument.setActive(true);
        }
    }

    private ResolvedProviderToken resolveProviderToken(PaymentMethod method, PaymentInstrumentRequest request) {
        String requestedToken = trimToNull(request.getProviderToken());
        String provider = method.getProvider() == null ? "" : method.getProvider().trim().toUpperCase(Locale.ROOT);
        String requestedTokenType = normalizeProviderTokenType(request.getProviderTokenType());

        if (requestedToken == null) {
            return new ResolvedProviderToken(null, null, null, null);
        }

        if ("PAYPAL".equals(provider) && "PAYPAL_SETUP_TOKEN".equals(requestedTokenType)) {
            PaymentMethodProviderConfigurationResolver.ResolvedPayPalConfig config = providerConfigurationResolver.resolvePayPal(method);
            PayPalClient.PayPalPaymentTokenResult paymentToken = payPalClient.createPaymentTokenFromSetupToken(config, requestedToken);
            String defaultDisplayLabel = paymentToken.customerEmail() != null && !paymentToken.customerEmail().isBlank()
                ? "PayPal - " + paymentToken.customerEmail()
                : method.getDisplayName();
            return new ResolvedProviderToken(
                paymentToken.paymentTokenId(),
                paymentToken.providerCustomerId(),
                defaultDisplayLabel,
                "PayPal"
            );
        }

        return new ResolvedProviderToken(requestedToken, null, null, null);
    }

    private void synchronizeDefaults(PaymentInstrument saved) {
        if (!Boolean.TRUE.equals(saved.getDefaultInstrument())) {
            return;
        }
        List<PaymentInstrument> others = paymentInstrumentRepository.findByCustomerIdAndDefaultInstrumentTrueAndIdNot(saved.getCustomerId(), saved.getId());
        if (others.isEmpty()) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        for (PaymentInstrument other : others) {
            other.setDefaultInstrument(false);
            other.setUpdatedAt(now);
        }
        paymentInstrumentRepository.saveAll(others);
    }

    private PaymentMethod resolveTokenizableMethod(String paymentMethodCode) {
        String normalizedCode = trimToNull(paymentMethodCode);
        if (normalizedCode == null) {
            throw new BadRequestException("Payment method is required");
        }
        PaymentMethod method = paymentMethodRepository.findByCode(normalizedCode)
            .filter(PaymentMethod::getActive)
            .orElseThrow(() -> new BadRequestException("Payment method not available: " + normalizedCode));
        String provider = method.getProvider() == null ? "" : method.getProvider().trim().toUpperCase(Locale.ROOT);
        String flow = method.getPaymentFlow() == null ? "" : method.getPaymentFlow().trim().toUpperCase(Locale.ROOT);
        if ("OFFLINE".equals(provider) || "OFFLINE".equals(flow)) {
            throw new BadRequestException("Offline payment methods cannot be tokenized");
        }
        boolean configured = switch (provider) {
            case "PAYPAL" -> providerConfigurationResolver.resolvePayPal(method).isAvailable();
            case "FABRICK" -> providerConfigurationResolver.resolveFabrick(method).isAvailable();
            default -> true;
        };
        if (!configured) {
            throw new BadRequestException("Payment method provider configuration is incomplete");
        }
        return method;
    }

    private PaymentInstrumentResponse toResponse(PaymentInstrument instrument) {
        PaymentInstrumentResponse response = new PaymentInstrumentResponse();
        response.setId(instrument.getId());
        response.setCustomerId(instrument.getCustomerId());
        response.setPaymentMethodCode(instrument.getPaymentMethodCode());
        response.setProvider(instrument.getProvider());
        response.setDisplayLabel(instrument.getDisplayLabel());
        response.setBrand(instrument.getBrand());
        response.setLast4(instrument.getLast4());
        response.setExpiryMonth(instrument.getExpiryMonth());
        response.setExpiryYear(instrument.getExpiryYear());
        response.setGatewayCustomerReference(instrument.getGatewayCustomerReference());
        response.setActive(instrument.getActive());
        response.setDefaultInstrument(instrument.getDefaultInstrument());
        response.setTokenStored(instrument.getProviderTokenEncrypted() != null && !instrument.getProviderTokenEncrypted().isBlank());
        response.setCreatedAt(instrument.getCreatedAt());
        response.setUpdatedAt(instrument.getUpdatedAt());
        return response;
    }

    private String fingerprint(String providerToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(providerToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to fingerprint payment token", ex);
        }
    }

    private String normalizeLast4(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String digits = trimmed.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }
        if (digits.length() > 4) {
            digits = digits.substring(digits.length() - 4);
        }
        return digits;
    }

    private Integer normalizeExpiryMonth(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 1 || value > 12) {
            throw new BadRequestException("Expiry month must be between 1 and 12");
        }
        return value;
    }

    private Integer normalizeExpiryYear(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 2000 || value > 2200) {
            throw new BadRequestException("Expiry year is invalid");
        }
        return value;
    }

    private String requiredTrimmed(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BadRequestException(field + " is required");
        }
        return trimmed;
    }

    private String normalizeProviderTokenType(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private record ResolvedProviderToken(
        String providerToken,
        String gatewayCustomerReference,
        String displayLabel,
        String brand
    ) {}
}
