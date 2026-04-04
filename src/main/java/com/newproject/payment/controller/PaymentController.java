package com.newproject.payment.controller;

import com.newproject.payment.dto.AdminPaymentMethodResponse;
import com.newproject.payment.dto.FabrickCompletionRequest;
import com.newproject.payment.dto.PaymentMethodRequest;
import com.newproject.payment.dto.PaymentMethodResponse;
import com.newproject.payment.dto.PaymentRefundRequest;
import com.newproject.payment.dto.PaymentRequest;
import com.newproject.payment.dto.PaymentResponse;
import com.newproject.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/methods")
    public List<PaymentMethodResponse> methods() {
        return paymentService.listMethods();
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
