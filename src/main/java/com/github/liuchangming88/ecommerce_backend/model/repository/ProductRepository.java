package com.github.liuchangming88.ecommerce_backend.model.repository;

import com.github.liuchangming88.ecommerce_backend.model.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

@Component
public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByName(@NotBlank(message = "Product name must not be empty") @Size(min = 1, max = 255, message = "Product name must be between 1 and 255 characters") @Pattern(regexp = "^[a-zA-Z0-9 ]*$", message = "Product name can only contain letters, numbers, and spaces") String name);
}
