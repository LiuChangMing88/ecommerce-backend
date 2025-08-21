package com.github.liuchangming88.ecommerce_backend.model.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "short_description", nullable = false)
    private String shortDescription;

    @Column(name = "long_description", length = 1024)
    private String longDescription;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @OneToOne(mappedBy = "product", cascade = {CascadeType.REMOVE, CascadeType.PERSIST, CascadeType.MERGE}, optional = false, orphanRemoval = true)
    private Inventory inventory;

}