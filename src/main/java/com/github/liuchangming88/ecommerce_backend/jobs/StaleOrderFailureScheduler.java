package com.github.liuchangming88.ecommerce_backend.jobs;

import com.github.liuchangming88.ecommerce_backend.configuration.OrderCleanupProperties;
import com.github.liuchangming88.ecommerce_backend.service.OrderRestockService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.OffsetDateTime;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class StaleOrderFailureScheduler {

    private static final Logger log = LoggerFactory.getLogger(StaleOrderFailureScheduler.class);

    private final OrderCleanupProperties props;
    private final OrderRestockService restockService;

    // Runs every 30 seconds (after previous execution completes)
    @Scheduled(fixedDelayString = "30000")
    public void run() {
        if (!props.isEnabled()) return;

        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(props.getFailureWindowMinutes());

        int total = 0;
        int batch;
        do {
            batch = restockService.failAndRestockStale(cutoff, props.getBatchSize());
            total += batch;
        } while (batch == props.getBatchSize()); // loop until fewer than batch size

        if (total > 0) {
            log.info("Stale order cleanup cycle finished. Total processed: {}", total);
        }
    }
}