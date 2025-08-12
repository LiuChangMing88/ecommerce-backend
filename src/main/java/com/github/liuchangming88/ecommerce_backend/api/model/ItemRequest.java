package com.github.liuchangming88.ecommerce_backend.api.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents one line item request when creating an order.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemRequest {
    @NotNull
    private Long productId;
    @Min(1)
    private int quantity;
}