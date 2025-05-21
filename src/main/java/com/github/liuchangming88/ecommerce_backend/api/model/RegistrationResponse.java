package com.github.liuchangming88.ecommerce_backend.api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RegistrationResponse {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String message;
}
