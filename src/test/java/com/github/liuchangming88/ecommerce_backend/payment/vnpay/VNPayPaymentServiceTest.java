package com.github.liuchangming88.ecommerce_backend.payment.vnpay;

import com.github.liuchangming88.ecommerce_backend.exception.AuthorizationException;
import com.github.liuchangming88.ecommerce_backend.model.LocalOrder;
import com.github.liuchangming88.ecommerce_backend.model.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.OrderStatus;
import com.github.liuchangming88.ecommerce_backend.payment.Payment;
import com.github.liuchangming88.ecommerce_backend.payment.PaymentRepository;
import com.github.liuchangming88.ecommerce_backend.payment.PaymentStatus;
import com.github.liuchangming88.ecommerce_backend.payment.dto.PayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VNPayPaymentService (initiateOrReusePayment).
 *
 * These are pure unit tests (no Spring context) with mocked repository & properties.
 */
public class VNPayPaymentServiceTest {

    private PaymentRepository paymentRepository;
    private VNPayProperties props;
    private VNPayPaymentService service;

    private static final Long USER_ID = 100L;
    private static final Long ORDER_ID = 555L;
    private static final BigDecimal ORDER_AMOUNT = new BigDecimal("123456"); // VND major units
    private static final String CURRENCY = "VND";
    private static final String CLIENT_IP = "203.0.113.42";
    private static final OffsetDateTime NOW = OffsetDateTime.now(ZoneOffset.UTC);
    private static final OffsetDateTime FUTURE_EXPIRY = NOW.plusMinutes(30);
    private static final OffsetDateTime PAST_EXPIRY = NOW.minusMinutes(1);

    @BeforeEach
    void setup() {
        paymentRepository = Mockito.mock(PaymentRepository.class);
        props = Mockito.mock(VNPayProperties.class);

        when(props.getVersion()).thenReturn("2.1.0");
        when(props.getCommand()).thenReturn("pay");
        when(props.getTmnCode()).thenReturn("DEMO_TMN");
        when(props.getCurrCode()).thenReturn(CURRENCY);
        when(props.getOrderType()).thenReturn("other");
        when(props.getReturnUrl()).thenReturn("https://example.com/return");
        when(props.getLocale()).thenReturn("vn");
        when(props.getPayUrl()).thenReturn("https://pay.vnpay.vn/payment");
        when(props.getHashSecret()).thenReturn("demoSecretHash");

        service = new VNPayPaymentService(paymentRepository, props);
    }

    /* --------------------------------- Helpers --------------------------------- */

    private LocalOrder newOrder(OrderStatus status, OffsetDateTime expiresAt) {
        LocalOrder order = new LocalOrder();
        order.setId(ORDER_ID);
        order.setStatus(status);
        order.setTotalAmount(ORDER_AMOUNT);
        order.setCurrency(CURRENCY);
        order.setExpiresAt(expiresAt);
        LocalUser user = new LocalUser();
        user.setId(USER_ID);
        order.setLocalUser(user);
        return order;
    }

    private Payment newPayment(String txnRef,
                               PaymentStatus status,
                               OffsetDateTime createdAt,
                               OffsetDateTime expiresAt) {
        Payment p = new Payment();
        p.setTxnRef(txnRef);
        p.setStatus(status);
        p.setAmount(ORDER_AMOUNT);
        p.setCurrency(CURRENCY);
        p.setClientIp(CLIENT_IP);
        p.setCreatedAt(createdAt);
        p.setExpiresAt(expiresAt);
        return p;
    }

    private Map<String,String> parseQueryParams(String url) {
        int idx = url.indexOf('?');
        assertThat(idx).isGreaterThanOrEqualTo(0);
        String qs = url.substring(idx + 1);
        Map<String,String> map = new LinkedHashMap<>();
        for (String pair : qs.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            String k = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            String v = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            map.put(k, v);
        }
        return map;
    }

    /* --------------------------------- Tests --------------------------------- */

    @Test
    @DisplayName("Ownership mismatch -> AuthorizationException")
    void ownershipMismatch() {
        LocalOrder order = newOrder(OrderStatus.PENDING, FUTURE_EXPIRY);
        order.getLocalUser().setId(USER_ID + 1); // different user

        assertThatThrownBy(() -> service.initiateOrReusePayment(order, CLIENT_IP, USER_ID))
                .isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName("Already PAID order -> IllegalStateException")
    void orderAlreadyPaid() {
        LocalOrder order = newOrder(OrderStatus.PAID, FUTURE_EXPIRY);

        assertThatThrownBy(() -> service.initiateOrReusePayment(order, CLIENT_IP, USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    @DisplayName("Order not in PENDING state -> IllegalStateException")
    void orderNotPending() {
        LocalOrder order = newOrder(OrderStatus.FAILED, FUTURE_EXPIRY);

        assertThatThrownBy(() -> service.initiateOrReusePayment(order, CLIENT_IP, USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not in PENDING");
    }

    @Test
    @DisplayName("Null total amount -> IllegalStateException")
    void nullTotalAmount() {
        LocalOrder order = newOrder(OrderStatus.PENDING, FUTURE_EXPIRY);
        order.setTotalAmount(null);

        assertThatThrownBy(() -> service.initiateOrReusePayment(order, CLIENT_IP, USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("total is null");
    }

    @Test
    @DisplayName("Non-positive total amount -> IllegalStateException")
    void nonPositiveAmount() {
        LocalOrder order = newOrder(OrderStatus.PENDING, FUTURE_EXPIRY);
        order.setTotalAmount(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.initiateOrReusePayment(order, CLIENT_IP, USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be positive");
    }

    @Test
    @DisplayName("Currency mismatch -> IllegalStateException")
    void currencyMismatch() {
        LocalOrder order = newOrder(OrderStatus.PENDING, FUTURE_EXPIRY);
        order.setCurrency("USD");

        assertThatThrownBy(() -> service.initiateOrReusePayment(order, CLIENT_IP, USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    @DisplayName("Existing SUCCEEDED payment blocks new initiation")
    void existingSucceededBlocks() {
        LocalOrder order = newOrder(OrderStatus.PENDING, FUTURE_EXPIRY);
        Payment existingSuccess = newPayment("REF-SUCCESS", PaymentStatus.SUCCEEDED, NOW.minusMinutes(10), FUTURE_EXPIRY);

        when(paymentRepository.findByOrderProviderAndStatuses(eq(ORDER_ID), eq("VNPAY"),
                argThat(list -> list.contains(PaymentStatus.INITIATED) && list.contains(PaymentStatus.SUCCEEDED))))
                .thenReturn(List.of(existingSuccess));

        assertThatThrownBy(() -> service.initiateOrReusePayment(order, CLIENT_IP, USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already has a successful payment");
    }

    @Test
    @DisplayName("Reuse existing non-expired INITIATED payment")
    void reuseExistingInitiated() {
        LocalOrder order = newOrder(OrderStatus.PENDING, FUTURE_EXPIRY);

        Payment reusable = newPayment("REF-REUSE-1", PaymentStatus.INITIATED,
                NOW.minusMinutes(5), FUTURE_EXPIRY);

        when(paymentRepository.findByOrderProviderAndStatuses(anyLong(), anyString(), anyList()))
                .thenReturn(List.of(reusable));

        // No new save expected (except maybe if repository.save used for new payment)
        PayResponse resp = service.initiateOrReusePayment(order, CLIENT_IP, USER_ID);

        assertThat(resp.getTxnRef()).isEqualTo("REF-REUSE-1");
        assertThat(resp.getRedirectUrl()).contains("vnp_TxnRef=REF-REUSE-1");
        assertThat(reusable.getStatus()).isEqualTo(PaymentStatus.INITIATED);

        // Ensure no save (new insertion) occurred
        verify(paymentRepository, times(0)).save(argThat(p -> p != reusable));
    }

    @Test
    @DisplayName("Expired INITIATED payment -> mark EXPIRED and create a new payment")
    void expiredInitiatedCreatesNew() {
        LocalOrder order = newOrder(OrderStatus.PENDING, FUTURE_EXPIRY);

        Payment expired = newPayment("REF-OLD-1", PaymentStatus.INITIATED,
                NOW.minusMinutes(40), PAST_EXPIRY);

        // Capture the newly saved payment
        AtomicReference<Payment> savedRef = new AtomicReference<>();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            // mimic JPA setting createdAt if your entity would do so (only if null)
            if (p.getCreatedAt() == null) {
                p.setCreatedAt(OffsetDateTime.now());
            }
            savedRef.set(p);
            return p;
        });

        when(paymentRepository.findByOrderProviderAndStatuses(anyLong(), anyString(), anyList()))
                .thenReturn(List.of(expired));

        PayResponse resp = service.initiateOrReusePayment(order, CLIENT_IP, USER_ID);

        // Old payment expired
        assertThat(expired.getStatus()).isEqualTo(PaymentStatus.EXPIRED);

        // New payment saved
        Payment newPayment = savedRef.get();
        assertThat(newPayment).isNotNull();
        assertThat(newPayment.getStatus()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(newPayment.getTxnRef()).isNotEqualTo("REF-OLD-1");
        assertThat(resp.getTxnRef()).isEqualTo(newPayment.getTxnRef());

        // Redirect URL query assertions
        Map<String,String> qp = parseQueryParams(resp.getRedirectUrl());
        assertThat(qp.get("vnp_TmnCode")).isEqualTo("DEMO_TMN");
        assertThat(qp.get("vnp_TxnRef")).isEqualTo(newPayment.getTxnRef());
        assertThat(qp.get("vnp_Amount")).isEqualTo(ORDER_AMOUNT.multiply(new BigDecimal("100")).toPlainString());
        assertThat(qp.get("vnp_OrderInfo")).isEqualTo("Order:" + ORDER_ID);
        assertThat(qp).containsKeys("vnp_SecureHash");
    }

    @Test
    @DisplayName("New payment insertion sets txnRef and returns PayResponse with expiresAt")
    void newPaymentInsertion() {
        LocalOrder order = newOrder(OrderStatus.PENDING, FUTURE_EXPIRY);

        when(paymentRepository.findByOrderProviderAndStatuses(anyLong(), anyString(), anyList()))
                .thenReturn(Collections.emptyList());

        // Capture saved
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            // simulate JPA createdAt
            if (p.getCreatedAt() == null) p.setCreatedAt(NOW);
            return p;
        });

        PayResponse resp = service.initiateOrReusePayment(order, CLIENT_IP, USER_ID);

        verify(paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();

        assertThat(saved.getTxnRef()).isEqualTo(resp.getTxnRef());
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(saved.getAmount()).isEqualByComparingTo(ORDER_AMOUNT);
        assertThat(saved.getCurrency()).isEqualTo(CURRENCY);
        assertThat(saved.getClientIp()).isEqualTo(CLIENT_IP);
        assertThat(resp.getRedirectUrl()).contains("vnp_TxnRef=" + saved.getTxnRef());

        Map<String,String> qp = parseQueryParams(resp.getRedirectUrl());
        assertThat(qp.get("vnp_ReturnUrl")).isEqualTo("https://example.com/return");
        assertThat(qp.get("vnp_Command")).isEqualTo("pay");
        assertThat(qp.get("vnp_OrderType")).isEqualTo("other");
        assertThat(qp.get("vnp_Locale")).isEqualTo("vn");
        assertThat(qp.get("vnp_CurrCode")).isEqualTo(CURRENCY);
        assertThat(qp.get("vnp_Amount")).isEqualTo(ORDER_AMOUNT.multiply(new BigDecimal("100")).toPlainString());
    }

    @Test
    @DisplayName("Reuse path does NOT generate a new payment")
    void reuseDoesNotSaveNewPayment() {
        LocalOrder order = newOrder(OrderStatus.PENDING, FUTURE_EXPIRY);

        Payment reusable = newPayment("REF-KEEP", PaymentStatus.INITIATED,
                NOW.minusMinutes(2), FUTURE_EXPIRY);

        when(paymentRepository.findByOrderProviderAndStatuses(anyLong(), anyString(), anyList()))
                .thenReturn(List.of(reusable));

        PayResponse resp = service.initiateOrReusePayment(order, CLIENT_IP, USER_ID);

        verify(paymentRepository, never()).save(any());
        assertThat(resp.getTxnRef()).isEqualTo("REF-KEEP");
    }

    @Nested
    @DisplayName("Edge & Formatting")
    class EdgeAndFormatting {

        @Test
        @DisplayName("Generated URL contains all required VNPay parameters")
        void generatedUrlContainsAllParams() {
            LocalOrder order = newOrder(OrderStatus.PENDING, FUTURE_EXPIRY);

            when(paymentRepository.findByOrderProviderAndStatuses(anyLong(), anyString(), anyList()))
                    .thenReturn(Collections.emptyList());

            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment p = invocation.getArgument(0);
                p.setCreatedAt(NOW);
                return p;
            });

            PayResponse resp = service.initiateOrReusePayment(order, CLIENT_IP, USER_ID);
            Map<String,String> qp = parseQueryParams(resp.getRedirectUrl());

            assertThat(qp.keySet()).contains(
                    "vnp_Version",
                    "vnp_Command",
                    "vnp_TmnCode",
                    "vnp_Amount",
                    "vnp_CurrCode",
                    "vnp_TxnRef",
                    "vnp_OrderInfo",
                    "vnp_OrderType",
                    "vnp_ReturnUrl",
                    "vnp_IpAddr",
                    "vnp_Locale",
                    "vnp_CreateDate"
            );
            // ExpireDate present because order has an expiresAt
            assertThat(qp).containsKey("vnp_ExpireDate");
            assertThat(qp).containsKey("vnp_SecureHash");
        }

        @Test
        @DisplayName("If order has no expiresAt, vnp_ExpireDate absent")
        void noExpireDate() {
            LocalOrder order = newOrder(OrderStatus.PENDING, null);

            when(paymentRepository.findByOrderProviderAndStatuses(anyLong(), anyString(), anyList()))
                    .thenReturn(Collections.emptyList());
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment p = invocation.getArgument(0);
                p.setCreatedAt(NOW);
                return p;
            });

            PayResponse resp = service.initiateOrReusePayment(order, CLIENT_IP, USER_ID);
            Map<String,String> qp = parseQueryParams(resp.getRedirectUrl());

            assertThat(qp).doesNotContainKey("vnp_ExpireDate");
        }
    }
}