package com.github.liuchangming88.ecommerce_backend.exception;

public class DuplicatedAddressException extends RuntimeException {
    public DuplicatedAddressException(String message) {
        super(message);
    }
}
