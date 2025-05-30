package com.github.liuchangming88.ecommerce_backend.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddressUpdateRequest {
    // TODO: Add restraints and messages
    @NotBlank(message = "Address line 1 must not be empty")
    @Size(max = 512, message = "Address line 1 must not be longer than 512 characters")
    private String addressLine1;

    @Size(max = 512, message = "Address line 2 must not be longer than 512 characters")
    private String addressLine2;

    @NotBlank(message = "City must not be empty")
    @Size(max = 255, message = "City must not be longer than 255 characters")
    private String city;

    @NotBlank(message = "Address line 1 must not be empty")
    @Size(max = 512, message = "Country must not be longer than 75 characters")
    private String country;
}
