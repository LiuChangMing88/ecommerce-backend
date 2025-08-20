package com.github.liuchangming88.ecommerce_backend.api.controller.order;

import com.github.liuchangming88.ecommerce_backend.api.model.CreateOrderRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.OrderResponse;
import com.github.liuchangming88.ecommerce_backend.model.user.LocalUser;
import com.github.liuchangming88.ecommerce_backend.service.order.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(path = "/orders")
public class OrderController {
    OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @AuthenticationPrincipal LocalUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OrderResponse> allOrdersList = orderService.getAllOrders(user.getId(), page, size);
        return new ResponseEntity<>(allOrdersList, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@AuthenticationPrincipal LocalUser user,
                                                     @RequestBody @Valid CreateOrderRequest request) {
        OrderResponse resp = orderService.createOrder(user, request);
        return ResponseEntity
                .created(URI.create("/orders/" + resp.getId()))
                .body(resp);
    }
}
