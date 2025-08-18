package com.github.liuchangming88.ecommerce_backend.payment.vnpay;

import com.github.liuchangming88.ecommerce_backend.model.order.LocalOrder;
import com.github.liuchangming88.ecommerce_backend.model.order.OrderStatus;
import com.github.liuchangming88.ecommerce_backend.payment.Payment;
import com.github.liuchangming88.ecommerce_backend.payment.PaymentRepository;
import com.github.liuchangming88.ecommerce_backend.payment.PaymentStatus;
import com.github.liuchangming88.ecommerce_backend.payment.dto.IpnResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class VNPayIpnServiceTest {

    private PaymentRepository paymentRepository;
    private VNPayProperties props;
    private VNPayIpnService ipnService;

    private static final String SECRET = "demoSecret";
    private static final String TMN_CODE = "DEMOTMN1";

    @BeforeEach
    void setup() {
        paymentRepository = Mockito.mock(PaymentRepository.class);
        props = Mockito.mock(VNPayProperties.class);
        when(props.getHashSecret()).thenReturn(SECRET);
        when(props.getTmnCode()).thenReturn(TMN_CODE);
        ipnService = new VNPayIpnService(paymentRepository, props);
    }

    // ---- Existing tests (unchanged) ----
    @Test
    void invalidSignature_returns97() {
        Map<String,String> params = baseParams("REF1", 100_000);
        params.put("vnp_SecureHash", "deadbeef");
        IpnResponse resp = ipnService.processIpnRequestAndReturnToVNPayServer(params);
        assertThat(resp.getRspCode()).isEqualTo("97");
    }

    @Test
    void unknownPayment_returns01() {
        Map<String,String> params = signedParams("UNKNOWN", 100_000, "00", "00");
        when(paymentRepository.findByTxnRef("UNKNOWN")).thenReturn(Optional.empty());
        IpnResponse resp = ipnService.processIpnRequestAndReturnToVNPayServer(params);
        assertThat(resp.getRspCode()).isEqualTo("01");
    }

    @Test
    void amountMismatch_returns04() {
        Payment p = payment("REF2", 200_000);
        when(paymentRepository.findByTxnRef("REF2")).thenReturn(Optional.of(p));
        Map<String,String> params = signedParams("REF2", 100_000, "00", "00");
        IpnResponse resp = ipnService.processIpnRequestAndReturnToVNPayServer(params);
        assertThat(resp.getRspCode()).isEqualTo("04");
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.INITIATED);
    }

    @Test
    void successFirstTime_updatesToSucceeded() {
        Payment p = payment("REF3", 150_000);
        when(paymentRepository.findByTxnRef("REF3")).thenReturn(Optional.of(p));
        Map<String,String> params = signedParams("REF3", 150_000, "00", "00");
        IpnResponse resp = ipnService.processIpnRequestAndReturnToVNPayServer(params);
        assertThat(resp.getRspCode()).isEqualTo("00");
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(p.getLocalOrder().getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(p.getResponseCode()).isEqualTo("00");
        assertThat(p.getTransactionNo()).isNotBlank();
        assertThat(p.getRawParams()).doesNotContain("vnp_SecureHash=");
    }

    @Test
    void duplicateSuccess_returns02_noChange() {
        Payment p = payment("REF4", 90_000);
        p.setStatus(PaymentStatus.SUCCEEDED);
        when(paymentRepository.findByTxnRef("REF4")).thenReturn(Optional.of(p));
        Map<String,String> params = signedParams("REF4", 90_000, "00", "00");
        IpnResponse resp = ipnService.processIpnRequestAndReturnToVNPayServer(params);
        assertThat(resp.getRspCode()).isEqualTo("02");
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void failure_setsFailed() {
        Payment p = payment("REF5", 42_000);
        when(paymentRepository.findByTxnRef("REF5")).thenReturn(Optional.of(p));
        Map<String,String> params = signedParams("REF5", 42_000, "24", "02");
        IpnResponse resp = ipnService.processIpnRequestAndReturnToVNPayServer(params);
        assertThat(resp.getRspCode()).isEqualTo("00");
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(p.getLocalOrder().getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(p.getResponseCode()).isEqualTo("24");
    }

    @Test
    void failedThenSuccessUpgrades() {
        Payment p = payment("REF6", 10_000);
        p.setStatus(PaymentStatus.FAILED);
        when(paymentRepository.findByTxnRef("REF6")).thenReturn(Optional.of(p));
        Map<String,String> params = signedParams("REF6", 10_000, "00", "00");
        IpnResponse resp = ipnService.processIpnRequestAndReturnToVNPayServer(params);
        assertThat(resp.getRspCode()).isEqualTo("00");
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void suspiciousMapsToSuspiciousIfEnumPresent() {
        Payment p = payment("REF7", 77_000);
        when(paymentRepository.findByTxnRef("REF7")).thenReturn(Optional.of(p));
        Map<String,String> params = signedParams("REF7", 77_000, "07", "00");
        IpnResponse resp = ipnService.processIpnRequestAndReturnToVNPayServer(params);
        assertThat(p.getStatus()).isIn(PaymentStatus.SUSPICIOUS, PaymentStatus.FAILED);
        assertThat(resp.getRspCode()).isEqualTo("00");
    }

    // ---- New tests to raise line & branch coverage ----

    @Test
    void merchantMismatch_returns01() {
        Map<String,String> params = baseParams("REF_MISS", 50_000);
        // valid signature but wrong tmnCode
        params.put("vnp_TmnCode", "OTHER");
        params.put("vnp_SecureHash", VNPaySigner.computeSignature(paramsWithoutHash(params), SECRET));
        IpnResponse resp = ipnService.processIpnRequestAndReturnToVNPayServer(params);
        assertThat(resp.getRspCode()).isEqualTo("01");
    }

    @Test
    void missingTxnRef_returns99() {
        Map<String,String> params = baseParams(null, 60_000);
        params.remove("vnp_TxnRef");
        params.put("vnp_SecureHash", VNPaySigner.computeSignature(paramsWithoutHash(params), SECRET));
        IpnResponse resp = ipnService.processIpnRequestAndReturnToVNPayServer(params);
        assertThat(resp.getRspCode()).isEqualTo("99"); // Missing txnRef path
    }

    @Test
    void missingSignature_returns97() {
        Map<String,String> params = baseParams("REF_SIG_MISS", 80_000);
        params.remove("vnp_SecureHash"); // no signature at all
        IpnResponse resp = ipnService.processIpnRequestAndReturnToVNPayServer(params);
        assertThat(resp.getRspCode()).isEqualTo("97");
    }

    @Test
    void nonDigitAmount_returns04() {
        Payment p = payment("REF_BAD_AMT", 11_000);
        when(paymentRepository.findByTxnRef("REF_BAD_AMT")).thenReturn(Optional.of(p));
        Map<String,String> params = baseParams("REF_BAD_AMT", 11_000);
        params.put("vnp_Amount", "12AB"); // non-digit
        params.put("vnp_SecureHash", VNPaySigner.computeSignature(paramsWithoutHash(params), SECRET));
        IpnResponse resp = ipnService.processIpnRequestAndReturnToVNPayServer(params);
        assertThat(resp.getRspCode()).isEqualTo("04");
    }

    @Test
    void fractionalAmountCausesArithmeticException_returns04() {
        Payment p = payment("REF_FRAC", 123_456);
        // set underlying amount with fraction so *100 -> non-integer longValueExact failure
        p.setAmount(new BigDecimal("1234.5678"));
        when(paymentRepository.findByTxnRef("REF_FRAC")).thenReturn(Optional.of(p));
        Map<String,String> params = signedParams("REF_FRAC", 1234, "00", "00");
        IpnResponse resp = ipnService.processIpnRequestAndReturnToVNPayServer(params);
        assertThat(resp.getRspCode()).isEqualTo("04");
    }

    @Test
    void failedThenFailedAgain_returns02() {
        Payment p = payment("REF_FAIL_AGAIN", 33_000);
        p.setStatus(PaymentStatus.FAILED); // terminal
        when(paymentRepository.findByTxnRef("REF_FAIL_AGAIN")).thenReturn(Optional.of(p));
        Map<String,String> params = signedParams("REF_FAIL_AGAIN", 33_000, "24", "02"); // still failure
        IpnResponse resp = ipnService.processIpnRequestAndReturnToVNPayServer(params);
        assertThat(resp.getRspCode()).isEqualTo("02");
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void successWithNullStatusTreatsAsInitiated() {
        Payment p = payment("REF_NULL_STATUS", 70_000);
        p.setStatus(null); // exercise null -> default INITIATED path
        when(paymentRepository.findByTxnRef("REF_NULL_STATUS")).thenReturn(Optional.of(p));
        Map<String,String> params = signedParams("REF_NULL_STATUS", 70_000, "00", "00");
        IpnResponse resp = ipnService.processIpnRequestAndReturnToVNPayServer(params);
        assertThat(resp.getRspCode()).isEqualTo("00");
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void successWithNullOrderDoesNotThrow() {
        Payment p = payment("REF_NULL_ORDER", 81_000);
        p.setLocalOrder(null); // exercise order==null branch
        when(paymentRepository.findByTxnRef("REF_NULL_ORDER")).thenReturn(Optional.of(p));
        Map<String,String> params = signedParams("REF_NULL_ORDER", 81_000, "00", "00");
        IpnResponse resp = ipnService.processIpnRequestAndReturnToVNPayServer(params);
        assertThat(resp.getRspCode()).isEqualTo("00");
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void successOrderAlreadyPaid_noStatusChange() {
        Payment p = payment("REF_ALREADY_PAID", 91_000);
        p.getLocalOrder().setStatus(OrderStatus.PAID);
        when(paymentRepository.findByTxnRef("REF_ALREADY_PAID")).thenReturn(Optional.of(p));
        Map<String,String> params = signedParams("REF_ALREADY_PAID", 91_000, "00", "00");
        IpnResponse resp = ipnService.processIpnRequestAndReturnToVNPayServer(params);
        assertThat(resp.getRspCode()).isEqualTo("00");
        assertThat(p.getLocalOrder().getStatus()).isEqualTo(OrderStatus.PAID); // unchanged
    }

    @Test
    void badPayDateLength_skipsParsing_noException() {
        Payment p = payment("REF_BAD_DATE", 55_000);
        when(paymentRepository.findByTxnRef("REF_BAD_DATE")).thenReturn(Optional.of(p));
        Map<String,String> params = signedParams("REF_BAD_DATE", 55_000, "00", "00");
        // corrupt pay date length (remove last char)
        params.put("vnp_PayDate", "2024010112003"); // 13 chars
        // recompute signature with modified params
        params.put("vnp_SecureHash", VNPaySigner.computeSignature(paramsWithoutHash(params), SECRET));
        IpnResponse resp = ipnService.processIpnRequestAndReturnToVNPayServer(params);
        assertThat(resp.getRspCode()).isEqualTo("00");
        // We can't assert paidAt via reflection here; just ensuring path doesn't blow
    }

    // ---- Helpers ----

    private Payment payment(String ref, long amountVnd) {
        Payment p = new Payment();
        p.setTxnRef(ref);
        p.setAmount(BigDecimal.valueOf(amountVnd));
        p.setStatus(PaymentStatus.INITIATED);
        LocalOrder o = new LocalOrder();
        o.setStatus(OrderStatus.PENDING);
        p.setLocalOrder(o);
        return p;
    }

    private Map<String,String> baseParams(String txnRef, long amountVnd) {
        Map<String,String> m = new HashMap<>();
        m.put("vnp_Version","2.1.0");
        m.put("vnp_TmnCode", TMN_CODE);
        if (txnRef != null) m.put("vnp_TxnRef", txnRef);
        m.put("vnp_Amount", String.valueOf(amountVnd * 100));
        m.put("vnp_OrderInfo","Test");
        m.put("vnp_ResponseCode","00");
        m.put("vnp_TransactionStatus","00");
        m.put("vnp_TransactionNo","123456");
        m.put("vnp_BankCode","NCB");
        m.put("vnp_PayDate","20240101120030");
        // signature added later
        return m;
    }

    private Map<String,String> signedParams(String txnRef, long amountVnd, String respCode, String txnStatus) {
        Map<String,String> m = baseParams(txnRef, amountVnd);
        m.put("vnp_ResponseCode", respCode);
        m.put("vnp_TransactionStatus", txnStatus);
        m.put("vnp_SecureHash", VNPaySigner.computeSignature(paramsWithoutHash(m), SECRET));
        return m;
    }

    private Map<String,String> paramsWithoutHash(Map<String,String> in) {
        Map<String,String> copy = new HashMap<>(in);
        copy.remove("vnp_SecureHash");
        return copy;
    }
}