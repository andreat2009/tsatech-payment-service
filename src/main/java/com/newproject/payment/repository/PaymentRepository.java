package com.newproject.payment.repository;

import com.newproject.payment.domain.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderId(Long orderId);

    Optional<Payment> findFirstByOrderIdOrderByIdAsc(Long orderId);
}
