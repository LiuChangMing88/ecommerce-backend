package com.github.liuchangming88.ecommerce_backend.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Simple DTO returned to client after initiating a VNPay payment.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PayResponse {
    private String redirectUrl;
    private String txnRef;
    private OffsetDateTime expiresAt;
}