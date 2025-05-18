package com.github.liuchangming88.ecommerce_backend.api.model;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationRequest {

    // These annotations are for AuthenticationController's @Valid checking. It will check if these fields satisfy the annotations
    // If the conditions are not met, it will throw an exception: MethodArgumentNotValidException. Which will be caught by GlobalExceptionHandler, and sent to the client.
    @NotBlank(message = "Username must not be empty")
    @Size(min = 3, max = 16, message = "Username must be between 3 and 16 characters")
    private String username;

    @NotBlank(message = "Password must not be empty")
    @Size(min = 8, max = 16, message = "Password must be between 8 and 16 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$", message = "Password must be minimum of eight characters, at least one uppercase letter, one lowercase letter and one number")
    private String password;

    @NotBlank(message = "Email must not be empty")
    @Email(message = "Email is not valid")
    @Size(max = 255, message = "Email must not be longer than 255 characters")
    private String email;

    @NotBlank(message = "First name must not be empty")
    @Size(max = 255, message = "First name must not be longer than 255 characters")
    private String firstName;

    @NotBlank(message = "Last name must not be empty")
    @Size(max = 255, message = "First name must not be longer than 255 characters")
    private String lastName;
}
