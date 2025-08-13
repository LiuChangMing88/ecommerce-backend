package com.github.liuchangming88.ecommerce_backend.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTxnRef(String txnRef);

    @Query("""
        SELECT p FROM Payment p
         WHERE p.localOrder.id = :orderId
           AND p.provider = :provider
           AND p.status IN :statuses
        """)
    List<Payment> findByOrderProviderAndStatuses(Long orderId, String provider, Collection<PaymentStatus> statuses);

    @Modifying
    @Query("""
        UPDATE Payment p
           SET p.status = :newStatus,
               p.responseCode = :respCode,
               p.transactionNo = :txnNo,
               p.bankCode = :bankCode,
               p.secureHash = :secureHash,
               p.rawParams = :rawParams,
               p.updatedAt = CURRENT_TIMESTAMP
         WHERE p.id = :id
           AND p.status = :expectedStatus
        """)
    int conditionalStatusUpdate(Long id,
                                PaymentStatus expectedStatus,
                                PaymentStatus newStatus,
                                String respCode,
                                String txnNo,
                                String bankCode,
                                String secureHash,
                                String rawParams);

    @Modifying
    @Query("""
        UPDATE Payment p
           SET p.status = com.github.liuchangming88.ecommerce_backend.payment.PaymentStatus.EXPIRED,
               p.updatedAt = CURRENT_TIMESTAMP
         WHERE p.status = com.github.liuchangming88.ecommerce_backend.payment.PaymentStatus.INITIATED
           AND p.expiresAt < :now
        """)
    int expireInitiatedBefore(OffsetDateTime now);
}