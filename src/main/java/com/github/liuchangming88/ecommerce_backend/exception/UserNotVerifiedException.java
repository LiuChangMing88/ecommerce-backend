package com.github.liuchangming88.ecommerce_backend.exception;

import lombok.Getter;

@Getter
public class UserNotVerifiedException extends RuntimeException {
    public UserNotVerifiedException(String message) {
        super(message);
    }
}
