package com.newproject.payment.service;

import com.newproject.payment.domain.Payment;
import com.newproject.payment.dto.PaymentRequest;
import com.newproject.payment.dto.PaymentResponse;
import com.newproject.payment.events.EventPublisher;
import com.newproject.payment.exception.NotFoundException;
import com.newproject.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final EventPublisher eventPublisher;

    public PaymentService(PaymentRepository paymentRepository, EventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PaymentResponse create(PaymentRequest request) {
        Payment payment = new Payment();
        applyRequest(payment, request);
        OffsetDateTime now = OffsetDateTime.now();
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        if (payment.getStatus() == null) {
            payment.setStatus("CREATED");
        }

        Payment saved = paymentRepository.save(payment);
        eventPublisher.publish("PAYMENT_CREATED", "payment", saved.getId().toString(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public PaymentResponse update(Long id, PaymentRequest request) {
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Payment not found"));

        applyRequest(payment, request);
        payment.setUpdatedAt(OffsetDateTime.now());

        Payment saved = paymentRepository.save(payment);
        eventPublisher.publish("PAYMENT_UPDATED", "payment", saved.getId().toString(), toResponse(saved));
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
        payment.setCurrency(currency);
        if (payment.getProvider() == null || payment.getProvider().isBlank()) {
            payment.setProvider("AUTO-KAFKA");
        }

        if (created) {
            payment.setCreatedAt(now);
            if (payment.getStatus() == null) {
                payment.setStatus("CREATED");
            }
        }

        payment.setUpdatedAt(now);

        Payment saved = paymentRepository.save(payment);
        eventPublisher.publish(created ? "PAYMENT_CREATED" : "PAYMENT_UPDATED", "payment", saved.getId().toString(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(Long id) {
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Payment not found"));
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> list(Long orderId) {
        if (orderId != null) {
            return paymentRepository.findByOrderId(orderId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        }
        return paymentRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Payment not found"));
        paymentRepository.delete(payment);
        eventPublisher.publish("PAYMENT_DELETED", "payment", id.toString(), null);
    }

    private void applyRequest(Payment payment, PaymentRequest request) {
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setStatus(request.getStatus() != null ? request.getStatus() : payment.getStatus());
        payment.setProvider(request.getProvider());
    }

    private PaymentResponse toResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setOrderId(payment.getOrderId());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setStatus(payment.getStatus());
        response.setProvider(payment.getProvider());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        return response;
    }
}
