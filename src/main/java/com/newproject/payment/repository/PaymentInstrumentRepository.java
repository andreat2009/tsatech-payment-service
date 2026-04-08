package com.newproject.payment.repository;

import com.newproject.payment.domain.PaymentInstrument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentInstrumentRepository extends JpaRepository<PaymentInstrument, Long> {
    List<PaymentInstrument> findByCustomerIdOrderByDefaultInstrumentDescCreatedAtDesc(Long customerId);
    Optional<PaymentInstrument> findByIdAndCustomerId(Long id, Long customerId);
    Optional<PaymentInstrument> findFirstByCustomerIdAndProviderTokenFingerprint(Long customerId, String providerTokenFingerprint);
    List<PaymentInstrument> findByCustomerIdAndDefaultInstrumentTrueAndIdNot(Long customerId, Long id);
}
