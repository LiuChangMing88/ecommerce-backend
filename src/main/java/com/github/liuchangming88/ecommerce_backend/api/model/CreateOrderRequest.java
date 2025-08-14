package com.github.liuchangming88.ecommerce_backend.api.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreateOrderRequest {

    @NotNull
    private Long addressId;

    @Size(min = 1, message = "At least one item required")
    @Valid
    private List<ItemRequest> items;
}