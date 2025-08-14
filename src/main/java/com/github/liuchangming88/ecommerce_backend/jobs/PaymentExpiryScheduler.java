package com.github.liuchangming88.ecommerce_backend.jobs;

import com.github.liuchangming88.ecommerce_backend.service.payment.PaymentMaintenanceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentExpiryScheduler.class);

    private final PaymentMaintenanceService paymentMaintenanceService;

    // Run every 30 seconds (configurable)
    @Scheduled(fixedDelayString = "30000")
    public void run() {
        try {
            int changed = paymentMaintenanceService.expireInitiatedPayments();
            if (changed == 0 && log.isDebugEnabled()) {
                log.debug("No payments expired this cycle");
            }
        } catch (Exception e) {
            log.error("Payment expiry task failed", e);
        }
    }
}