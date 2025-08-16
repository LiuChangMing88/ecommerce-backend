package com.github.liuchangming88.ecommerce_backend.jobs;

import com.github.liuchangming88.ecommerce_backend.configuration.OrderCleanupProperties;
import com.github.liuchangming88.ecommerce_backend.service.order.OrderRestockService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class StaleOrderFailureScheduler {

    private static final Logger log = LoggerFactory.getLogger(StaleOrderFailureScheduler.class);

    private final OrderCleanupProperties props;
    private final OrderRestockService restockService;

    @Scheduled(fixedDelayString = "${orders.cleanup.delay-ms:30000}")
    public void run() {
        if (!props.isEnabled()) return;

        int total = 0;
        int batch;
        // Capture a consistent 'now' for the entire loop pass; prevents a long run from drifting
        OffsetDateTime now = OffsetDateTime.now();
        do {
            batch = restockService.failAndRestockExpired(now, props.getBatchSize());
            total += batch;
            // Optional: refresh 'now' each iteration if you WANT drifting boundary
            // now = OffsetDateTime.now();
        } while (batch == props.getBatchSize());

        if (total > 0) {
            log.info("Order expiry cycle finished. Total processed: {}", total);
        }
    }
}