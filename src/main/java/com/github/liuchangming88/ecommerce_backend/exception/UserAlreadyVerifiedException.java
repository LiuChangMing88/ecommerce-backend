package com.github.liuchangming88.ecommerce_backend.exception;

// user already verified...
public class UserAlreadyVerifiedException extends RuntimeException {
    public UserAlreadyVerifiedException(String message) {
        super(message);
    }
}
