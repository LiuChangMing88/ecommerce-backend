package com.github.liuchangming88.ecommerce_backend.payment.vnpay;

import com.github.liuchangming88.ecommerce_backend.model.LocalOrder;
import com.github.liuchangming88.ecommerce_backend.model.OrderStatus;
import com.github.liuchangming88.ecommerce_backend.payment.Payment;
import com.github.liuchangming88.ecommerce_backend.payment.PaymentRepository;
import com.github.liuchangming88.ecommerce_backend.payment.PaymentStatus;
import com.github.liuchangming88.ecommerce_backend.payment.dto.IpnResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * VNPay IPN processing service

 * Responsibilities:
 *  1. Extract & canonicalize vnp_* params
 *  2. Verify signature
 *  3. Validate merchant (tmnCode), presence of txnRef, payment existence
 *  4. Validate amount (exact integer *100)
 *  5. Map provider codes to internal status (success / suspicious / failed)
 *  6. Idempotency & upgrade logic (FAILED/EXPIRED -> SUCCEEDED; never downgrade SUCCEEDED)
 *  7. Persist provider fields; update related order
 *  8. Return spec-compliant RspCode

 * Return codes used (per spec):
 *   00 - Confirm Success (updated or accepted)
 *   01 - Order/Payment not found (or wrong tmnCode)
 *   02 - Order already confirmed (terminal & unchanged)
 *   04 - Invalid amount
 *   97 - Invalid signature
 *   99 - Unknown internal error
 */
@Service
public class VNPayIpnService {

    private static final Logger log = LoggerFactory.getLogger(VNPayIpnService.class);

    // Spec response codes
    private static final String RC_SUCCESS             = "00";
    private static final String RC_ALREADY_CONFIRMED   = "02";
    private static final String RC_NOT_FOUND           = "01";
    private static final String RC_INVALID_AMOUNT      = "04";
    private static final String RC_INVALID_SIGNATURE   = "97";
    private static final String RC_UNKNOWN_ERROR       = "99";

    // Provider success & suspicious codes
    private static final String PROVIDER_SUCCESS_CODE  = "00";
    private static final String PROVIDER_SUSPICIOUS    = "07";

    private static final DateTimeFormatter PAYDATE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PaymentRepository paymentRepository;
    private final VNPayProperties props;

    public VNPayIpnService(PaymentRepository paymentRepository, VNPayProperties props) {
        this.paymentRepository = paymentRepository;
        this.props = props;
    }

    /**
     * Entry point called by controller.
     * All exceptions are caught to always return a JSON response to VNPay.
     */
    public IpnResponse processIpnRequestAndReturnToVNPayServer(Map<String, String> rawQueryParams) {
        long start = System.nanoTime();
        try {
            // 1. Extract canonical VNPay parameters & signature
            ExtractResult extract = extractAndValidateSignature(rawQueryParams);
            if (!extract.signatureValid) {
                log.warn("[VNPay][IPN] Invalid signature txnRef={} raw={}", extract.txnRef, rawQueryParams);
                return rsp(RC_INVALID_SIGNATURE, "Invalid signature");
            }

            // 2. Merchant validation (tmnCode) + txnRef presence
            ValidationContext ctx = validateMerchantAndReference(extract);
            if (!ctx.valid) return ctx.toIpnResponse();

            // 3. Load payment
            Payment payment = loadPayment(ctx.txnRef);
            if (payment == null) return rsp(RC_NOT_FOUND, "Payment not found");

            // 4. Amount validation
            AmountValidation amountVal = validateAmount(payment, extract.vnpParams.get("vnp_Amount"));
            if (!amountVal.valid) return rsp(RC_INVALID_AMOUNT, "Invalid amount");

            // 5. Derive target status from provider codes
            ProviderOutcome outcome = deriveProviderOutcome(extract.vnpParams);

            // 6. Idempotency & upgrade decision
            IdempotencyDecision decision = decideIdempotency(payment, outcome);
            if (decision.alreadyConfirmed) {
                return rsp(RC_ALREADY_CONFIRMED, "Order already confirmed");
            }

            // 7. Apply updates (payment + order)
            applyUpdate(payment, outcome, extract);

            return rsp(RC_SUCCESS, "Confirm Success");
        } catch (Exception e) {
            log.error("[VNPay][IPN] Unexpected error", e);
            return rsp(RC_UNKNOWN_ERROR, "Unknown error");
        } finally {
            long durMs = (System.nanoTime() - start) / 1_000_000;
            if (durMs > 500) {
                log.info("[VNPay][IPN] Processed in {} ms (slow path)", durMs);
            }
        }
    }

    /* =========================================================
       Extraction & Signature
       ========================================================= */

    private ExtractResult extractAndValidateSignature(Map<String, String> raw) {
        Map<String,String> vnpParams = raw.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().startsWith("vnp_"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Pull signature fields
        String receivedHash = vnpParams.remove("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHashType");

        String txnRef = vnpParams.get("vnp_TxnRef"); // may be null at this stage
        if (receivedHash == null || receivedHash.isBlank()) {
            return new ExtractResult(vnpParams, txnRef, false);
        }

        boolean signatureValid = VNPaySigner.verify(rebuildParamMapForSigner(vnpParams, receivedHash), props.getHashSecret());
        // We pass a map including vnp_SecureHash for verify() usage pattern; verify() re-extracts appropriately.

        return new ExtractResult(vnpParams, txnRef, signatureValid);
    }

    /**
     * Some existing VNPaySigner.verify implementation expects the signature still inside.
     * We rebuild a temp map with signature re-attached for that method.
     */
    private Map<String,String> rebuildParamMapForSigner(Map<String,String> vnpParams, String signature) {
        Map<String,String> temp = new HashMap<>(vnpParams);
        temp.put("vnp_SecureHash", signature);
        return temp;
    }

    /* =========================================================
       Merchant & Reference Validation
       ========================================================= */

    private ValidationContext validateMerchantAndReference(ExtractResult extract) {
        String tmnCode = extract.vnpParams.get("vnp_TmnCode");
        if (tmnCode == null || !tmnCode.equals(props.getTmnCode())) {
            return ValidationContext.invalid(RC_NOT_FOUND, "Payment not found (tmnCode mismatch)", extract.txnRef);
        }
        if (extract.txnRef == null || extract.txnRef.isBlank()) {
            return ValidationContext.invalid(RC_UNKNOWN_ERROR, "Missing txnRef", extract.txnRef);
        }
        return ValidationContext.valid(extract.txnRef);
    }

    /* =========================================================
       Payment & Amount
       ========================================================= */

    private Payment loadPayment(String txnRef) {
        return paymentRepository.findByTxnRef(txnRef).orElse(null);
    }

    private AmountValidation validateAmount(Payment payment, String vnpAmountStr) {
        if (vnpAmountStr == null || !vnpAmountStr.matches("\\d+")) {
            return AmountValidation.invalid();
        }
        long receivedMinor;
        try {
            receivedMinor = Long.parseLong(vnpAmountStr);
        } catch (NumberFormatException ex) {
            return AmountValidation.invalid();
        }

        long expectedMinor;
        try {
            expectedMinor = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValueExact();
        } catch (ArithmeticException ex) {
            return AmountValidation.invalid();
        }

        return receivedMinor == expectedMinor ? AmountValidation.valid() : AmountValidation.invalid();
    }

    /* =========================================================
       Outcome Mapping & Idempotency
       ========================================================= */

    private ProviderOutcome deriveProviderOutcome(Map<String,String> vnpParams) {
        String respCode    = vnpParams.get("vnp_ResponseCode");
        String txnStatus   = vnpParams.get("vnp_TransactionStatus");
        boolean success    = PROVIDER_SUCCESS_CODE.equals(respCode) && PROVIDER_SUCCESS_CODE.equals(txnStatus);
        boolean suspicious = PROVIDER_SUSPICIOUS.equals(respCode);

        PaymentStatus mapped;
        if (success) {
            mapped = PaymentStatus.SUCCEEDED;
        } else if (suspicious && supportsSuspicious()) {
            mapped = PaymentStatus.SUSPICIOUS;
        } else {
            mapped = PaymentStatus.FAILED;
        }
        return new ProviderOutcome(respCode, txnStatus, mapped, success, suspicious);
    }

    private boolean supportsSuspicious() {
        try {
            PaymentStatus.valueOf("SUSPICIOUS");
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private IdempotencyDecision decideIdempotency(Payment payment, ProviderOutcome outcome) {
        PaymentStatus current = payment.getStatus();
        if (current == null) current = PaymentStatus.INITIATED;

        boolean terminal = current.isTerminal();
        if (!terminal) {
            // Not terminal -> proceed normally
            return IdempotencyDecision.proceed();
        }

        // Allow upgrade (FAILED or EXPIRED -> SUCCEEDED)
        if (outcome.mappedStatus == PaymentStatus.SUCCEEDED &&
                current != PaymentStatus.SUCCEEDED &&
                (current == PaymentStatus.FAILED || current.name().equals("EXPIRED"))) {
            log.info("[VNPay][IPN] Upgrading payment {} from {} -> SUCCEEDED", payment.getTxnRef(), current);
            return IdempotencyDecision.proceed();
        }

        // Already terminal in same or different non-upgradable way
        return IdempotencyDecision.alreadyConfirmed();
    }

    /* =========================================================
       Apply Update
       ========================================================= */

    private void applyUpdate(Payment payment, ProviderOutcome outcome, ExtractResult extract) {
        // Provider fields
        payment.setResponseCode(outcome.responseCode);
        setProviderStatusIfPresent(payment, outcome.transactionStatus);
        payment.setTransactionNo(extract.vnpParams.get("vnp_TransactionNo"));
        payment.setBankCode(extract.vnpParams.get("vnp_BankCode"));
        payment.setRawParams(serializeParams(extract.vnpParams)); // No signature included

        if (outcome.success) {
            payment.setStatus(PaymentStatus.SUCCEEDED);
            setPaidAtIfPresent(payment, extract.vnpParams.get("vnp_PayDate"));
            // Order update
            LocalOrder order = payment.getLocalOrder();
            if (order != null && order.getStatus() != OrderStatus.PAID) {
                order.setStatus(OrderStatus.PAID);
            }
        } else if (outcome.mappedStatus == PaymentStatus.SUSPICIOUS) {
            payment.setStatus(PaymentStatus.SUSPICIOUS);
            LocalOrder order = payment.getLocalOrder();
            if (order != null && order.getStatus() != OrderStatus.PAID) {
                order.setStatus(OrderStatus.FAILED); // or a dedicated SUSPICIOUS order status
            }
            setFailureCodeIfPresent(payment, outcome.responseCode);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            LocalOrder order = payment.getLocalOrder();
            if (order != null && order.getStatus() != OrderStatus.PAID) {
                order.setStatus(OrderStatus.FAILED);
            }
            setFailureCodeIfPresent(payment, outcome.responseCode);
        }

        log.info("[VNPay][IPN] txnRef={} outcome={} respCode={} providerStatus={} newStatus={}",
                payment.getTxnRef(),
                outcome.success ? "SUCCESS" : outcome.mappedStatus,
                outcome.responseCode,
                outcome.transactionStatus,
                payment.getStatus());
    }

    /* =========================================================
       Helper Setters (no reflection ideally)
       ========================================================= */

    private void setProviderStatusIfPresent(Payment payment, String providerStatus) {
        try {
            payment.getClass().getMethod("setProviderStatus", String.class)
                    .invoke(payment, providerStatus);
        } catch (Exception ignored) {
            // If you add providerStatus to Payment, replace this reflection with direct call.
        }
    }

    private void setFailureCodeIfPresent(Payment payment, String failureCode) {
        try {
            payment.getClass().getMethod("setFailureCode", String.class)
                    .invoke(payment, failureCode);
        } catch (Exception ignored) {}
    }

    private void setPaidAtIfPresent(Payment payment, String payDateStr) {
        if (payDateStr == null || payDateStr.length() != 14) return;
        try {
            LocalDateTime ldt = LocalDateTime.parse(payDateStr, PAYDATE_FMT);
            Instant instant = ldt.atZone(VN_ZONE).toInstant();
            payment.getClass().getMethod("setPaidAt", Instant.class)
                    .invoke(payment, instant);
        } catch (DateTimeParseException | ReflectiveOperationException ignored) { }
    }

    /* =========================================================
       Serialization / Responses
       ========================================================= */

    private String serializeParams(Map<String,String> m) {
        return m.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    private IpnResponse rsp(String code, String message) {
        return new IpnResponse(code, message);
    }

    /* =========================================================
       DTO / Helper Inner Classes
       ========================================================= */

    private static class ExtractResult {
        final Map<String,String> vnpParams;
        final String txnRef;
        final boolean signatureValid;
        ExtractResult(Map<String,String> vnpParams, String txnRef, boolean signatureValid) {
            this.vnpParams = vnpParams;
            this.txnRef = txnRef;
            this.signatureValid = signatureValid;
        }
    }

    private static class ValidationContext {
        final boolean valid;
        final String rspCode;
        final String message;
        final String txnRef;

        private ValidationContext(boolean valid, String rspCode, String message, String txnRef) {
            this.valid = valid;
            this.rspCode = rspCode;
            this.message = message;
            this.txnRef = txnRef;
        }
        static ValidationContext valid(String txnRef) {
            return new ValidationContext(true, null, null, txnRef);
        }
        static ValidationContext invalid(String code, String msg, String txnRef) {
            return new ValidationContext(false, code, msg, txnRef);
        }
        IpnResponse toIpnResponse() {
            return new IpnResponse(rspCode, message);
        }
    }

    private static class AmountValidation {
        final boolean valid;
        private AmountValidation(boolean valid) { this.valid = valid; }
        static AmountValidation valid() { return new AmountValidation(true); }
        static AmountValidation invalid() { return new AmountValidation(false); }
    }

    private static class ProviderOutcome {
        final String responseCode;
        final String transactionStatus;
        final PaymentStatus mappedStatus;
        final boolean success;
        final boolean suspicious;
        ProviderOutcome(String responseCode, String transactionStatus,
                        PaymentStatus mappedStatus, boolean success, boolean suspicious) {
            this.responseCode = responseCode;
            this.transactionStatus = transactionStatus;
            this.mappedStatus = mappedStatus;
            this.success = success;
            this.suspicious = suspicious;
        }
    }

    private static class IdempotencyDecision {
        final boolean alreadyConfirmed;
        private IdempotencyDecision(boolean alreadyConfirmed) {
            this.alreadyConfirmed = alreadyConfirmed;
        }
        static IdempotencyDecision proceed() { return new IdempotencyDecision(false); }
        static IdempotencyDecision alreadyConfirmed() { return new IdempotencyDecision(true); }
    }
}