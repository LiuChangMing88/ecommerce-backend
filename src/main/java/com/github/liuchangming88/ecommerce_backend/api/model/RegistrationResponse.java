package com.github.liuchangming88.ecommerce_backend.api.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationResponse {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String message;
}
