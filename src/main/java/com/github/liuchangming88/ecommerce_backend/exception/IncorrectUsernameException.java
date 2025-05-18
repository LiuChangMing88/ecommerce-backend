package com.github.liuchangming88.ecommerce_backend.exception;

import org.springframework.security.core.AuthenticationException;

public class IncorrectUsernameException extends AuthenticationException {
    public IncorrectUsernameException(String message) {
        super(message);
    }
}
