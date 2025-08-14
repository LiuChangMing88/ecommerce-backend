package com.github.liuchangming88.ecommerce_backend.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Simple DTO returned to VNPay after processing IPN request
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IpnResponse {
    String RspCode;
    String Message;
}
