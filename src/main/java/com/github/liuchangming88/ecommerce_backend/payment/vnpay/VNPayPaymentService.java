package com.github.liuchangming88.ecommerce_backend.payment.vnpay;

import com.github.liuchangming88.ecommerce_backend.exception.AuthorizationException;
import com.github.liuchangming88.ecommerce_backend.model.LocalOrder;
import com.github.liuchangming88.ecommerce_backend.model.OrderStatus;
import com.github.liuchangming88.ecommerce_backend.payment.Payment;
import com.github.liuchangming88.ecommerce_backend.payment.PaymentRepository;
import com.github.liuchangming88.ecommerce_backend.payment.PaymentStatus;
import com.github.liuchangming88.ecommerce_backend.payment.dto.PayResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class VNPayPaymentService {

    private final PaymentRepository paymentRepository;
    private final VNPayProperties props;
    private static final ZoneId HCM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final DateTimeFormatter VNP_DT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public VNPayPaymentService(PaymentRepository paymentRepository, VNPayProperties props) {
        this.paymentRepository = paymentRepository;
        this.props = props;
    }

    @Transactional
    public PayResponse initiateOrReusePayment(LocalOrder order, String clientIp, Long userId) {
        // 1. Ownership + order invariants
        if (!Objects.equals(userId, order.getLocalUser().getId()))
            throw new AuthorizationException("That order doesn't belong to you!");
        if (order.getStatus() == OrderStatus.PAID)
            throw new IllegalStateException("Order already paid");
        if (order.getStatus() != OrderStatus.PENDING)
            throw new IllegalStateException("Order not in PENDING state (current=" + order.getStatus() + ")");
        if (order.getTotalAmount() == null)
            throw new IllegalStateException("Order total is null");
        if (order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalStateException("Order total must be positive");
        if (!order.getCurrency().equalsIgnoreCase(props.getCurrCode()))
            throw new IllegalStateException("Currency mismatch: order=" + order.getCurrency() + " expected=" + props.getCurrCode());

        OffsetDateTime now = OffsetDateTime.now();

        // 2. Fetch blocking payments (INITIATED or SUCCEEDED). SUCCEEDED always blocks; INITIATED may be reused or expired.
        List<Payment> blocking = paymentRepository.findByOrderProviderAndStatuses(
                order.getId(), "VNPAY", List.of(PaymentStatus.INITIATED, PaymentStatus.SUCCEEDED));

        // 3. Handle existing successful payment
        if (blocking.stream().anyMatch(p -> p.getStatus() == PaymentStatus.SUCCEEDED)) {
            // You can either throw or return a special response
            throw new IllegalStateException("Order already has a successful payment");
        }

        // 4. Find reusable INITIATED
        Payment reusable = null;
        for (Payment p : blocking) {
            if (p.getStatus() == PaymentStatus.INITIATED) {
                boolean expired = p.getExpiresAt() != null && !p.getExpiresAt().isAfter(now); // <= now means expired
                if (expired) {
                    p.setStatus(PaymentStatus.EXPIRED); // Will flush before new insert because of transactional boundary
                } else {
                    reusable = p;  // Keep last non-expired (there should be at most one anyway if DB constraint is in place)
                }
            }
        }

        if (reusable != null) {
            // Rebuild redirect using original createdAt and original expiresAt
            String redirect = buildVNPayRedirectUrl(
                    reusable.getTxnRef(),
                    order.getId(),
                    reusable.getAmount(),
                    reusable.getCurrency(),
                    reusable.getClientIp(),
                    reusable.getCreatedAt(),
                    reusable.getExpiresAt()
            );
            return new PayResponse(redirect, reusable.getTxnRef(), reusable.getExpiresAt());
        }

        String txnRef = generateTxnRef(order.getId());
        // Create payment row
        Payment p = new Payment();
        p.setLocalOrder(order);
        p.setAmount(order.getTotalAmount());
        p.setCurrency(order.getCurrency());
        p.setTxnRef(txnRef);
        p.setStatus(PaymentStatus.INITIATED);
        p.setClientIp(clientIp);
        p.setExpiresAt(order.getExpiresAt());
        paymentRepository.save(p);

        String redirect = buildVNPayRedirectUrl(
                p.getTxnRef(),
                order.getId(),
                p.getAmount(),
                p.getCurrency(),
                p.getClientIp(),
                p.getCreatedAt(),
                p.getExpiresAt()
        );
        return new PayResponse(redirect, txnRef, order.getExpiresAt());
    }

    private String toMinorUnits(BigDecimal amount) {
        // VNPay expects integer amount *100 (even for VND)
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0).toPlainString();
    }

    private String generateTxnRef(Long orderId) {
        long epoch = System.currentTimeMillis();
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        // length: keep well under 64 chars
        return orderId + "-" + epoch + "-" + rand;
    }

    private String buildVNPayRedirectUrl(String txnRef,
                                         Long orderId,
                                         BigDecimal amount,
                                         String currency,
                                         String clientIp,
                                         OffsetDateTime createdAt,
                                         OffsetDateTime expiresAt) {

        Map<String,String> params = new HashMap<>();
        params.put("vnp_Version", props.getVersion());
        params.put("vnp_Command", props.getCommand());
        params.put("vnp_TmnCode", props.getTmnCode());
        params.put("vnp_Amount", toMinorUnits(amount));
        params.put("vnp_CurrCode", props.getCurrCode());
        params.put("vnp_TxnRef", txnRef);
        // Use a parseable order marker. Keep consistent with IPN validation.
        params.put("vnp_OrderInfo", "Order:" + orderId);
        params.put("vnp_OrderType", props.getOrderType());
        params.put("vnp_ReturnUrl", props.getReturnUrl());
        params.put("vnp_IpAddr", clientIp);
        params.put("vnp_Locale", props.getLocale());
        params.put("vnp_CreateDate", createdAt.atZoneSameInstant(HCM_ZONE).format(VNP_DT));
        if (expiresAt != null) {
            params.put("vnp_ExpireDate", expiresAt.atZoneSameInstant(HCM_ZONE).format(VNP_DT));
        }

        String query = VNPaySigner.buildQueryStringWithHash(params, props.getHashSecret());
        return props.getPayUrl() + "?" + query;
    }
}