package com.github.liuchangming88.ecommerce_backend.service;

import com.github.liuchangming88.ecommerce_backend.exception.ResourceNotFoundException;
import com.github.liuchangming88.ecommerce_backend.model.*;
import com.github.liuchangming88.ecommerce_backend.model.repository.LocalOrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderRestockService {

    private static final Logger log = LoggerFactory.getLogger(OrderRestockService.class);

    private final LocalOrderRepository localOrderRepository;

    /**
     * Process one batch of stale PENDING orders. Returns how many orders were restocked.
     */
    @Transactional
    public int failAndRestockExpired(OffsetDateTime now, int batchSize) {
        List<Long> ids = localOrderRepository.findExpiredPendingOrderIds(now, org.springframework.data.domain.PageRequest.of(0, batchSize));
        if (ids.isEmpty()) return 0;

        int processed = 0;
        for (Long id : ids) {
            LocalOrder order = localOrderRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Order " + id + " vanished during expiry pass"));

            // Double‑check (race safety) with current state
            if (order.getStatus() != OrderStatus.PENDING ||
                    order.isRestocked() ||
                    order.getExpiresAt() == null ||
                    !order.getExpiresAt().isBefore(now)) {
                continue;
            }

            // OPTIONAL: guard against active payment (uncomment if you implement Strategy 3)
            // if (paymentRepository.existsByLocalOrderIdAndStatusAndExpiresAtAfter(order.getId(), PaymentStatus.INITIATED, now)) {
            //     continue;
            // }

            order.setStatus(OrderStatus.FAILED);
            restock(order);
            processed++;
        }

        if (processed > 0) {
            log.info("Order expiry: failed & restocked {} orders (now={}, batchSize={})", processed, now, batchSize);
        }
        return processed;
    }

    private void restock(LocalOrder order) {
        if (order.isRestocked()) return; // idempotent
        for (LocalOrderItems line : order.getItems()) {
            Product p = line.getProduct();
            Inventory inventory = p.getInventory();
            inventory.setQuantity(inventory.getQuantity() + line.getQuantity());
        }
        order.setRestocked(true);
    }
}