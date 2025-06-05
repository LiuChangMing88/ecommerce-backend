package com.github.liuchangming88.ecommerce_backend.exception;

// Namely invalid token, expired token...
public class TokenException extends RuntimeException {
    public TokenException(String message) {
        super(message);
    }
}
