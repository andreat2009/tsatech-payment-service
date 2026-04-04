package com.newproject.payment.repository;

import com.newproject.payment.domain.PaymentMethod;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    Optional<PaymentMethod> findByCode(String code);

    List<PaymentMethod> findByActiveTrueOrderBySortOrderAscCodeAsc();

    List<PaymentMethod> findAllByOrderBySortOrderAscCodeAsc();
}
