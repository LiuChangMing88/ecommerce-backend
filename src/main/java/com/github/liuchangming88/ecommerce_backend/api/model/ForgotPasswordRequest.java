package com.github.liuchangming88.ecommerce_backend.api.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequest {
    @NotBlank(message = "Email must not be empty")
    @Email(message = "Email is not valid")
    @Size(max = 255, message = "Email must not be longer than 255 characters")
    private String email;
}
