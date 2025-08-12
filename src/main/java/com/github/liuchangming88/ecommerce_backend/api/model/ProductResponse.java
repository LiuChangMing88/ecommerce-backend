package com.github.liuchangming88.ecommerce_backend.api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private String shortDescription;
    private String longDescription;
    private BigDecimal price;
    private Long quantity;
}
