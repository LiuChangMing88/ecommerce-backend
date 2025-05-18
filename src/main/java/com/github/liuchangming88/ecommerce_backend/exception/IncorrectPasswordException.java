package com.github.liuchangming88.ecommerce_backend.exception;


import org.springframework.security.core.AuthenticationException;

public class IncorrectPasswordException extends AuthenticationException {
    public IncorrectPasswordException(String message) {
        super(message);
    }
}