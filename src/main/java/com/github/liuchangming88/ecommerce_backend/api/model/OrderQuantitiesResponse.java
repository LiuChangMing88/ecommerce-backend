package com.github.liuchangming88.ecommerce_backend.api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderQuantitiesResponse {
    private Long id;
    private Long productId;
    private String productName;
    private Integer quantity;
}
