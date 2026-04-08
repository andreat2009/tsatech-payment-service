package com.newproject.payment.controller;

import com.newproject.payment.dto.AdminPaymentMethodResponse;
import com.newproject.payment.dto.FabrickCompletionRequest;
import com.newproject.payment.dto.PayPalBrowserVaultSessionResponse;
import com.newproject.payment.dto.PayPalSetupTokenResponse;
import com.newproject.payment.dto.PaymentInstrumentRequest;
import com.newproject.payment.dto.PaymentInstrumentResponse;
import com.newproject.payment.dto.PaymentMethodRequest;
import com.newproject.payment.dto.PaymentMethodResponse;
import com.newproject.payment.dto.PaymentRefundRequest;
import com.newproject.payment.dto.PaymentRequest;
import com.newproject.payment.dto.PaymentResponse;
import com.newproject.payment.service.PaymentInstrumentService;
import com.newproject.payment.service.PaymentService;
import com.newproject.payment.service.PaymentVaultService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;
    private final PaymentInstrumentService paymentInstrumentService;
    private final PaymentVaultService paymentVaultService;

    public PaymentController(
        PaymentService paymentService,
        PaymentInstrumentService paymentInstrumentService,
        PaymentVaultService paymentVaultService
    ) {
        this.paymentService = paymentService;
        this.paymentInstrumentService = paymentInstrumentService;
        this.paymentVaultService = paymentVaultService;
    }

    @GetMapping("/methods")
    public List<PaymentMethodResponse> methods() {
        return paymentService.listMethods();
    }

    @GetMapping("/customers/{customerId}/instruments")
    public List<PaymentInstrumentResponse> listPaymentInstruments(@PathVariable Long customerId) {
        return paymentInstrumentService.listForCustomer(customerId);
    }

    @PostMapping("/customers/{customerId}/instruments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentInstrumentResponse createPaymentInstrument(@PathVariable Long customerId, @RequestBody PaymentInstrumentRequest request) {
        return paymentInstrumentService.create(customerId, request);
    }

    @PutMapping("/customers/{customerId}/instruments/{instrumentId}")
    public PaymentInstrumentResponse updatePaymentInstrument(
        @PathVariable Long customerId,
        @PathVariable Long instrumentId,
        @RequestBody PaymentInstrumentRequest request
    ) {
        return paymentInstrumentService.update(customerId, instrumentId, request);
    }

    @DeleteMapping("/customers/{customerId}/instruments/{instrumentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePaymentInstrument(@PathVariable Long customerId, @PathVariable Long instrumentId) {
        paymentInstrumentService.delete(customerId, instrumentId);
    }

    @PostMapping("/customers/{customerId}/vault/paypal/{paymentMethodCode}/browser-session")
    public PayPalBrowserVaultSessionResponse createPayPalBrowserVaultSession(
        @PathVariable Long customerId,
        @PathVariable String paymentMethodCode
    ) {
        return paymentVaultService.createPayPalBrowserVaultSession(customerId, paymentMethodCode);
    }

    @PostMapping("/customers/{customerId}/vault/paypal/{paymentMethodCode}/setup-token")
    public PayPalSetupTokenResponse createPayPalSetupToken(
        @PathVariable Long customerId,
        @PathVariable String paymentMethodCode
    ) {
        return paymentVaultService.createPayPalSetupToken(customerId, paymentMethodCode);
    }

    @GetMapping("/admin/methods")
    public List<AdminPaymentMethodResponse> listAdminMethods() {
        return paymentService.listAdminMethods();
    }

    @GetMapping("/admin/methods/{id}")
    public AdminPaymentMethodResponse getAdminMethod(@PathVariable Long id) {
        return paymentService.getAdminMethod(id);
    }

    @PostMapping("/admin/methods")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminPaymentMethodResponse createAdminMethod(@Valid @RequestBody PaymentMethodRequest request) {
        return paymentService.createAdminMethod(request);
    }

    @PutMapping("/admin/methods/{id}")
    public AdminPaymentMethodResponse updateAdminMethod(@PathVariable Long id, @Valid @RequestBody PaymentMethodRequest request) {
        return paymentService.updateAdminMethod(id, request);
    }

    @DeleteMapping("/admin/methods/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAdminMethod(@PathVariable Long id) {
        paymentService.deleteAdminMethod(id);
    }

    @GetMapping
    public List<PaymentResponse> list(@RequestParam(value = "orderId", required = false) Long orderId) {
        return paymentService.list(orderId);
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable Long id) {
        return paymentService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@Valid @RequestBody PaymentRequest request) {
        return paymentService.create(request);
    }

    @PostMapping("/{id}/capture/paypal")
    public PaymentResponse capturePayPal(@PathVariable Long id, @RequestParam(value = "token", required = false) String token) {
        return paymentService.capturePayPal(id, token);
    }

    @PostMapping("/{id}/complete/fabrick")
    public PaymentResponse completeFabrick(@PathVariable Long id, @RequestBody FabrickCompletionRequest request) {
        return paymentService.completeFabrick(id, request);
    }

    @PostMapping("/{id}/refund")
    public PaymentResponse refund(@PathVariable Long id, @RequestBody(required = false) PaymentRefundRequest request) {
        return paymentService.refund(id, request);
    }

    @PostMapping("/{id}/reconcile")
    public PaymentResponse reconcile(@PathVariable Long id) {
        return paymentService.reconcile(id);
    }

    @PostMapping("/webhooks/paypal")
    public Map<String, Object> handlePayPalWebhook(
        @RequestHeader("PAYPAL-TRANSMISSION-ID") String transmissionId,
        @RequestHeader("PAYPAL-TRANSMISSION-TIME") String transmissionTime,
        @RequestHeader("PAYPAL-CERT-URL") String certUrl,
        @RequestHeader("PAYPAL-AUTH-ALGO") String authAlgo,
        @RequestHeader("PAYPAL-TRANSMISSION-SIG") String transmissionSig,
        @RequestBody String body
    ) {
        return paymentService.handlePayPalWebhook(transmissionId, transmissionTime, certUrl, authAlgo, transmissionSig, body);
    }

    @PostMapping("/webhooks/fabrick")
    public Map<String, Object> handleFabrickWebhook(
        @RequestParam Map<String, String> parameters,
        @RequestBody(required = false) String body
    ) {
        return paymentService.handleFabrickWebhook(parameters, body);
    }

    @PutMapping("/{id}")
    public PaymentResponse update(@PathVariable Long id, @Valid @RequestBody PaymentRequest request) {
        return paymentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        paymentService.delete(id);
    }
}
