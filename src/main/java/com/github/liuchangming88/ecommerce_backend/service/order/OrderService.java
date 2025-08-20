package com.github.liuchangming88.ecommerce_backend.service.order;

import com.github.liuchangming88.ecommerce_backend.api.model.*;
import com.github.liuchangming88.ecommerce_backend.exception.AuthorizationException;
import com.github.liuchangming88.ecommerce_backend.exception.InsufficientStockException;
import com.github.liuchangming88.ecommerce_backend.exception.ResourceNotFoundException;
import com.github.liuchangming88.ecommerce_backend.model.order.LocalOrder;
import com.github.liuchangming88.ecommerce_backend.model.order.LocalOrderItems;
import com.github.liuchangming88.ecommerce_backend.model.order.OrderStatus;
import com.github.liuchangming88.ecommerce_backend.model.product.Product;
import com.github.liuchangming88.ecommerce_backend.model.user.Address;
import com.github.liuchangming88.ecommerce_backend.model.user.repository.AddressRepository;
import com.github.liuchangming88.ecommerce_backend.model.product.repository.InventoryRepository;
import com.github.liuchangming88.ecommerce_backend.model.order.repository.LocalOrderRepository;
import com.github.liuchangming88.ecommerce_backend.model.product.repository.ProductRepository;
import com.github.liuchangming88.ecommerce_backend.model.user.LocalUser;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final Integer orderExpiryTimeInMinutes = 15;

    private final InventoryRepository inventoryRepository;
    LocalOrderRepository localOrderRepository;
    ProductRepository productRepository;
    AddressRepository addressRepository;
    ModelMapper modelMapper;

    public OrderService(LocalOrderRepository localOrderRepository, ProductRepository productRepository, AddressRepository addressRepository, ModelMapper modelMapper, InventoryRepository inventoryRepository) {
        this.localOrderRepository = localOrderRepository;
        this.productRepository = productRepository;
        this.addressRepository = addressRepository;
        this.modelMapper = modelMapper;
        this.inventoryRepository = inventoryRepository;
    }

    public Page<OrderResponse> getAllOrders(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<LocalOrder> localOrdersPage = localOrderRepository.findByLocalUser_Id(userId, pageable);

        return localOrdersPage.map(order -> {
            // Map items
            List<OrderItemsResponse> orderItemsResponseList = order.getItems().stream()
                    .map(q -> modelMapper.map(q, OrderItemsResponse.class))
                    .toList();

            // Map address
            AddressResponse addressResponse = modelMapper.map(order.getAddress(), AddressResponse.class);

            // Map order itself
            OrderResponse orderResponse = modelMapper.map(order, OrderResponse.class);
            orderResponse.setItems(orderItemsResponseList);
            orderResponse.setAddressResponse(addressResponse);

            return orderResponse;
        });
    }

    @Transactional
    public OrderResponse createOrder(LocalUser user, CreateOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("No items provided");
        }

        // 1. Address ownership check
        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new EntityNotFoundException("Address " + request.getAddressId()));
        if (address.getLocalUser() == null || !address.getLocalUser().getId().equals(user.getId())) {
            throw new AuthorizationException("Address does not belong to authenticated user");
        }

        // 2. Validate and collect product IDs (enforce uniqueness)
        validateItems(request.getItems());
        Set<Long> productIds = request.getItems().stream()
                .map(ItemRequest::getProductId)
                .collect(Collectors.toSet());

        // 3. Fetch all products in a single query
        Map<Long, Product> productMap = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // Ensure all exist
        for (ItemRequest ir : request.getItems()) {
            if (!productMap.containsKey(ir.getProductId())) {
                throw new ResourceNotFoundException("Product " + ir.getProductId());
            }
        }

        // 4. Inventory reservation (decrement) section
        for (ItemRequest ir : request.getItems()) {
            if (ir.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be > 0 for product " + ir.getProductId());
            }
        }

        // Sort to avoid deadlocks due to different ordering (if ever using row locks)
        List<ItemRequest> ordered = new ArrayList<>(request.getItems());
        ordered.sort(Comparator.comparing(ItemRequest::getProductId));
        for (ItemRequest ir : ordered) {
            boolean ok = inventoryRepository.decrementIfAvailable(ir.getProductId(), ir.getQuantity());
            if (!ok) {
                throw new InsufficientStockException("Product with id " + ir.getProductId() + " doesn't have enough stock. " +
                        "Current stock: " + productMap.get(ir.getProductId()).getInventory().getQuantity() +
                        ". Quantity you wanted to buy: " + ir.getQuantity());
            }
        }

        // 5. Build LocalOrder
        LocalOrder order = new LocalOrder();
        order.setLocalUser(user);
        order.setAddress(address);
        order.setStatus(OrderStatus.PENDING);
        order.setCurrency("VND");
        order.setRestocked(false);
        order.setCreatedAt(OffsetDateTime.now());
        order.setExpiresAt(OffsetDateTime.now().plusMinutes(orderExpiryTimeInMinutes));

        BigDecimal total = BigDecimal.ZERO;

        for (ItemRequest ir : request.getItems()) {
            Product product = productMap.get(ir.getProductId());

            BigDecimal unitPrice = extractPrice(product);
            // Snapshot the price
            LocalOrderItems line = new LocalOrderItems();
            line.setProduct(product);
            line.setQuantity(ir.getQuantity());
            line.setUnitPrice(unitPrice);

            // maintain bi-directional integrity if needed
            order.addItem(line);

            total = total.add(unitPrice.multiply(BigDecimal.valueOf(ir.getQuantity())));
        }

        // 6. Persist
        order.setTotalAmount(total.setScale(2));
        LocalOrder saved = localOrderRepository.save(order);

        // 7. Map to OrderResponse using ModelMapper for base fields
        OrderResponse response = modelMapper.map(saved, OrderResponse.class);

        // 8. Map Address -> AddressResponse (if the type map didn't already handle)
        if (response.getAddressResponse() == null && saved.getAddress() != null) {
            response.setAddressResponse(modelMapper.map(saved.getAddress(), AddressResponse.class));
        }

        // 9. Map line items manually (ModelMapper with lazy collections is risky)
        List<OrderItemsResponse> lineDtos = new ArrayList<>();
        for (LocalOrderItems line : saved.getItems()) {
            OrderItemsResponse l = new OrderItemsResponse(
                    line.getId(),
                    line.getProduct().getId(),
                    line.getProduct().getName(),
                    line.getQuantity(),
                    line.getUnitPrice(),
                    line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()))
            );
            lineDtos.add(l);
        }
        response.setItems(lineDtos);

        return response;
    }

    private void validateItems(List<ItemRequest> items) {
        Set<Long> seen = new HashSet<>();
        for (ItemRequest ir : items) {
            if (ir.getProductId() == null) {
                throw new IllegalArgumentException("Product id cannot be null");
            }
            if (!seen.add(ir.getProductId())) {
                throw new IllegalArgumentException("Duplicate product id: " + ir.getProductId());
            }
            if (ir.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be > 0 for product " + ir.getProductId());
            }
        }
    }

    private BigDecimal extractPrice(Product product) {
        BigDecimal price = product.getPrice();
        if (price == null) {
            throw new IllegalStateException("Price not set for product " + product.getId());
        }
        // Normalize to 2 decimals (assuming VND – normally 0 decimals, but you snapshot as scale 2)
        return price.setScale(2);
    }
}
