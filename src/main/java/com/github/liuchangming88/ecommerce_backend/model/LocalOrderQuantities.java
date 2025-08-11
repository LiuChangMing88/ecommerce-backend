package com.github.liuchangming88.ecommerce_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "local_order_quantities")
public class LocalOrderQuantities {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Min(1)
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // Snapshot unit price at order time. Never recalc from Product during IPN.
    @NotNull
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @ManyToOne(optional = false)
    @JoinColumn(name = "local_order_id", nullable = false)
    private LocalOrder localOrder;
}