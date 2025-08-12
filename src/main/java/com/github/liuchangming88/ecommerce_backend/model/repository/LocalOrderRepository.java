package com.github.liuchangming88.ecommerce_backend.model.repository;

import com.github.liuchangming88.ecommerce_backend.model.LocalOrder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public interface LocalOrderRepository extends JpaRepository<LocalOrder, Long> {

    List<LocalOrder> findByLocalUser_Id(Long id);

    /**
     * Returns IDs of stale PENDING orders (not yet restocked) older than cutoff.
     * This is for the scheduler to find PENDING orders that have exceeded the time limit then mark them as FAILED and restock the product inventories
     */
    @Query("""
        SELECT o.id
        FROM LocalOrder o
        WHERE o.status = com.github.liuchangming88.ecommerce_backend.model.OrderStatus.PENDING
          AND o.restocked = false
          AND o.createdAt < :cutoff
        ORDER BY o.createdAt ASC
        """)
    List<Long> findStalePendingOrderIds(OffsetDateTime cutoff, Pageable pageable);
}
