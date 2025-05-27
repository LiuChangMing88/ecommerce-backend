package com.github.liuchangming88.ecommerce_backend.exception;

public class DuplicatedUserException extends RuntimeException {
    public DuplicatedUserException(String message) {
        super(message);
    }
}
