package com.github.liuchangming88.ecommerce_backend.service.payment;

import com.github.liuchangming88.ecommerce_backend.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class PaymentMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(PaymentMaintenanceService.class);

    private final PaymentRepository paymentRepository;

    /**
     * Expires INITIATED payments whose expiresAt is in the past.
     * Transactional boundary sits here; caller (scheduler, admin endpoint, test) needs none.
     *
     * @return number of rows transitioned to EXPIRED
     */
    @Transactional
    public int expireInitiatedPayments() {
        OffsetDateTime now = OffsetDateTime.now();
        int changed = paymentRepository.expireInitiatedBefore(now);
        if (changed > 0) {
            log.info("Expired {} payments (cutoff={})", changed, now);
        }
        return changed;
    }
}