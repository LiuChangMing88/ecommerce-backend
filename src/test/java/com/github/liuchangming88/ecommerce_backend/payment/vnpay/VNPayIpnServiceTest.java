package com.github.liuchangming88.ecommerce_backend.payment.vnpay;

import com.github.liuchangming88.ecommerce_backend.model.LocalOrder;
import com.github.liuchangming88.ecommerce_backend.model.OrderStatus;
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

/**
 * Pure unit tests for VNPayIpnService (no Spring context).
 */
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

    @Test
    void invalidSignature_returns97() {
        Map<String,String> params = baseParams("REF1", 100_000);
        // Corrupt signature
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

        // Send mismatch amount (expects 200k, send 100k)
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

        Map<String,String> params = signedParams("REF5", 42_000, "24", "02"); // user cancelled maybe

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

        // If you kept SUSPICIOUS enum
        assertThat(p.getStatus()).isIn(PaymentStatus.SUSPICIOUS, PaymentStatus.FAILED);
        assertThat(resp.getRspCode()).isEqualTo("00");
    }

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
        m.put("vnp_TxnRef", txnRef);
        m.put("vnp_Amount", String.valueOf(amountVnd * 100)); // *100 minor
        m.put("vnp_OrderInfo","Test");
        m.put("vnp_ResponseCode","00");
        m.put("vnp_TransactionStatus","00");
        m.put("vnp_TransactionNo","123456");
        m.put("vnp_BankCode","NCB");
        m.put("vnp_PayDate","20240101120030");
        return m;
    }

    private Map<String,String> signedParams(String txnRef, long amountVnd, String respCode, String txnStatus) {
        Map<String,String> m = baseParams(txnRef, amountVnd);
        m.put("vnp_ResponseCode", respCode);
        m.put("vnp_TransactionStatus", txnStatus);
        String sig = VNPaySigner.computeSignature(m, SECRET);
        m.put("vnp_SecureHash", sig);
        return m;
    }
}