package com.newproject.payment.repository;

import com.newproject.payment.domain.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    List<Payment> findByOrderId(Long orderId);

    Optional<Payment> findFirstByOrderIdOrderByIdAsc(Long orderId);

    Optional<Payment> findFirstByProviderOrderId(String providerOrderId);

    Optional<Payment> findFirstByProviderPaymentId(String providerPaymentId);

    Optional<Payment> findFirstByLightboxPaymentToken(String lightboxPaymentToken);
}
