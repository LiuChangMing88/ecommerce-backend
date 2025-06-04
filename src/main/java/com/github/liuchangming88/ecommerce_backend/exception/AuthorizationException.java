package com.github.liuchangming88.ecommerce_backend.exception;

// user not verified
public class AuthorizationException extends RuntimeException {
    public AuthorizationException(String message) {
        super(message);
    }
}
