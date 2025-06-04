package com.github.liuchangming88.ecommerce_backend.exception;

// Namely incorrect password, incorrect username, user not verified, user already verified...
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}
