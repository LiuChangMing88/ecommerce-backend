package com.github.liuchangming88.ecommerce_backend.exception;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
