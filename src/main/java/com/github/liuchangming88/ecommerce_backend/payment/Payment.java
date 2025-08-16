package com.github.liuchangming88.ecommerce_backend.payment;

import com.github.liuchangming88.ecommerce_backend.model.order.LocalOrder;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "payment", indexes = {
        @Index(name = "uk_payment_txn_ref", columnList = "txn_ref", unique = true)
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which order this payment is for
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "local_order_id", nullable = false)
    private LocalOrder localOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Column(name = "provider", nullable = false, length = 16)
    private String provider = "VNPAY";

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    // VNPay fields
    @Column(name = "txn_ref", nullable = false, length = 64)
    private String txnRef;

    @Column(name = "transaction_no", length = 32)
    private String transactionNo;

    @Column(name = "response_code", length = 8)
    private String responseCode;

    @Column(name = "bank_code", length = 32)
    private String bankCode;

    @Lob
    @Column(name = "raw_params")
    private String rawParams; // full query string or JSON of IPN/return for audit

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PreUpdate
    public void touch() { this.updatedAt = OffsetDateTime.now(); }
}