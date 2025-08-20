package com.github.liuchangming88.ecommerce_backend.model.order.repository;

import com.github.liuchangming88.ecommerce_backend.model.order.LocalOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public interface LocalOrderRepository extends JpaRepository<LocalOrder, Long> {

    List<LocalOrder> findByLocalUser_Id(Long id);
    Page<LocalOrder> findByLocalUser_Id(Long userId, Pageable pageable);

    /**
     * Returns IDs of PENDING orders whose expiresAt has passed and not yet restocked.
     * Ordered by expiresAt to process oldest expirations first.
     */
    @Query("""
        SELECT o.id
        FROM LocalOrder o
        WHERE o.status = com.github.liuchangming88.ecommerce_backend.model.order.OrderStatus.PENDING
          AND o.restocked = false
          AND o.expiresAt < :now
        ORDER BY o.expiresAt ASC
        """)
    List<Long> findExpiredPendingOrderIds(OffsetDateTime now, Pageable pageable);
}
