package com.github.liuchangming88.ecommerce_backend.api.model;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequest {
    @NotBlank(message = "Product name must not be empty")
    @Size(min = 1, max = 255, message = "Product name must be between 1 and 255 characters")
    @Pattern(regexp = "^[a-zA-Z0-9 ]*$", message = "Product name can only contain letters, numbers, and spaces")
    private String name;

    @NotBlank(message = "Short description must not be empty.")
    @Size(min = 1, max = 255, message = "Short description must be between 1 and 255 characters")
    private String shortDescription;

    @Size(min = 1, max = 1024, message = "Long description must be between 1 and 1024 characters")
    private String longDescription;

    @NotNull(message = "Price must not be empty.")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero.")
    @Digits(integer = 10, fraction = 2, message = "Price can only have up to 10 integer digits and 2 decimal places")
    private Double price;

    @NotNull(message = "Quantity must not be empty.")
    @Min(value = 0, message = "Quantity cannot be negative.")
    private Long quantity;
}
