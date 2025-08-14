package com.github.liuchangming88.ecommerce_backend.payment;

public enum PaymentStatus {
    INITIATED,
    PENDING,
    SUCCEEDED,
    FAILED,
    EXPIRED,
    SUSPICIOUS;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == EXPIRED || this == SUSPICIOUS;
    }
}