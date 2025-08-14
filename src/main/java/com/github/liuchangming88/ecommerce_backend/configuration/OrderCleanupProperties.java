package com.github.liuchangming88.ecommerce_backend.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Properties controlling automatic failure / restock of stale pending orders.
 */
@Component
@ConfigurationProperties(prefix = "order.cleanup")
@Getter
@Setter
public class OrderCleanupProperties {
    /**
     * Number of orders processed per batch iteration.
     */
    private int batchSize = 200;

    /**
     * Whether the scheduler is enabled.
     */
    private boolean enabled = true;
}