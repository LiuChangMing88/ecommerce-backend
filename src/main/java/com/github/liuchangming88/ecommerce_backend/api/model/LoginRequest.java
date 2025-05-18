package com.github.liuchangming88.ecommerce_backend.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "Username must not be empty")
    @Size(min = 3, max = 16, message = "Username must be between 3 and 16 characters")
    private String username;

    @NotBlank(message = "Password must not be empty")
    @Size(min = 8, max = 16, message = "Password must be between 8 and 16 characters")
    private String password;
}
