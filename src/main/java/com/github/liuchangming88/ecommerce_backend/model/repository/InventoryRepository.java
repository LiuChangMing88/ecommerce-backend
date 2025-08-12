package com.github.liuchangming88.ecommerce_backend.model.repository;

import com.github.liuchangming88.ecommerce_backend.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    @Modifying(clearAutomatically = false, flushAutomatically = false)
    @Query("""
        UPDATE Inventory i
           SET i.quantity = i.quantity - :qty
         WHERE i.product.id = :pid
           AND i.quantity >= :qty
        """)
    int decrementIfAvailableInternal(@Param("pid") Long productId,
                                     @Param("qty") int qty);

    default boolean decrementIfAvailable(Long productId, int qty) {
        if (qty <= 0) throw new IllegalArgumentException("Quantity must be > 0 (was " + qty + ")");
        return decrementIfAvailableInternal(productId, qty) == 1;
    }
}