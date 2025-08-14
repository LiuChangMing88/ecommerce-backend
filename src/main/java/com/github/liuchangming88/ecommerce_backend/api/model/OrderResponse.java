package com.github.liuchangming88.ecommerce_backend.api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private Long id;
    private String status;
    private BigDecimal totalAmount;
    private String currency;
    private OffsetDateTime createdAt;
    private AddressResponse addressResponse;
    private List<OrderItemsResponse> items;
}
