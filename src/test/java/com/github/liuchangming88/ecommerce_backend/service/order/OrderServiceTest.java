package com.github.liuchangming88.ecommerce_backend.service.order;

import com.github.liuchangming88.ecommerce_backend.api.model.*;
import com.github.liuchangming88.ecommerce_backend.exception.AuthorizationException;
import com.github.liuchangming88.ecommerce_backend.exception.InsufficientStockException;
import com.github.liuchangming88.ecommerce_backend.exception.ResourceNotFoundException;
import com.github.liuchangming88.ecommerce_backend.model.order.OrderStatus;
import com.github.liuchangming88.ecommerce_backend.model.product.Inventory;
import com.github.liuchangming88.ecommerce_backend.model.product.Product;
import com.github.liuchangming88.ecommerce_backend.model.product.repository.InventoryRepository;
import com.github.liuchangming88.ecommerce_backend.model.product.repository.ProductRepository;
import com.github.liuchangming88.ecommerce_backend.model.user.Address;
import com.github.liuchangming88.ecommerce_backend.model.order.LocalOrder;
import com.github.liuchangming88.ecommerce_backend.model.order.LocalOrderItems;
import com.github.liuchangming88.ecommerce_backend.model.order.repository.LocalOrderRepository;
import com.github.liuchangming88.ecommerce_backend.model.user.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.user.repository.AddressRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class OrderServiceTest {

    @Mock LocalOrderRepository localOrderRepository;
    @Mock ProductRepository productRepository;
    @Mock AddressRepository addressRepository;
    @Mock InventoryRepository inventoryRepository;
    @Mock ModelMapper modelMapper;

    @InjectMocks OrderService orderService;

    LocalUser user;
    Address address;

    @BeforeEach
    void init() {
        user = new LocalUser();
        user.setId(10L);
        address = new Address();
        address.setId(55L);
        address.setLocalUser(user);
    }

    // Existing getAllOrders test (kept, slightly cleaned)
    @Test
    void getAllOrders_withValidUserId_returnsMappedResponses() {
        Long userId = 10L;

        // Prepare mock order
        LocalOrderItems line = new LocalOrderItems();
        LocalOrder order = new LocalOrder();
        order.setItems(List.of(line));
        order.setAddress(new Address());

        // Page containing our single order
        Pageable pageable = PageRequest.of(0, 10);
        Page<LocalOrder> orderPage = new PageImpl<>(List.of(order), pageable, 1);

        when(localOrderRepository.findByLocalUser_Id(userId, pageable)).thenReturn(orderPage);

        // Prepare DTO mappings
        OrderItemsResponse lineDto = new OrderItemsResponse();
        AddressResponse addrDto = new AddressResponse();
        OrderResponse orderDto = new OrderResponse();

        when(modelMapper.map(line, OrderItemsResponse.class)).thenReturn(lineDto);
        when(modelMapper.map(order.getAddress(), AddressResponse.class)).thenReturn(addrDto);
        when(modelMapper.map(order, OrderResponse.class)).thenReturn(orderDto);

        // Call service
        Page<OrderResponse> result = orderService.getAllOrders(userId, 0, 10);

        // Assertions
        Assertions.assertEquals(1, result.getTotalElements());
        OrderResponse mappedOrder = result.getContent().get(0);

        Assertions.assertEquals(orderDto, mappedOrder);
        Assertions.assertEquals(List.of(lineDto), mappedOrder.getItems());
        Assertions.assertEquals(addrDto, mappedOrder.getAddressResponse());
    }


    // -------- Negative branches for createOrder --------

    @Test
    void createOrder_nullRequest() {
        assertThatThrownBy(() -> orderService.createOrder(user, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Request cannot be null");
    }

    @Test
    void createOrder_noItems() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setAddressId(address.getId());
        req.setItems(Collections.emptyList());
        assertThatThrownBy(() -> orderService.createOrder(user, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No items");
    }

    @Test
    void createOrder_addressNotFound() {
        CreateOrderRequest req = baseReq(List.of(item(1L,1)));
        when(addressRepository.findById(address.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.createOrder(user, req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Address " + address.getId());
    }

    @Test
    void createOrder_addressNotOwned() {
        LocalUser other = new LocalUser(); other.setId(999L);
        address.setLocalUser(other);
        CreateOrderRequest req = baseReq(List.of(item(1L,1)));
        when(addressRepository.findById(address.getId())).thenReturn(Optional.of(address));
        assertThatThrownBy(() -> orderService.createOrder(user, req))
                .isInstanceOf(AuthorizationException.class);
    }

    @Test
    void createOrder_duplicateProductId() {
        CreateOrderRequest req = baseReq(List.of(item(1L,2), item(1L,3)));
        when(addressRepository.findById(address.getId())).thenReturn(Optional.of(address));
        assertThatThrownBy(() -> orderService.createOrder(user, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate product id: 1");
    }

    @Test
    void createOrder_nullProductId() {
        ItemRequest ir = new ItemRequest();
        ir.setProductId(null);
        ir.setQuantity(1);
        CreateOrderRequest req = baseReq(List.of(ir));
        when(addressRepository.findById(address.getId())).thenReturn(Optional.of(address));
        assertThatThrownBy(() -> orderService.createOrder(user, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product id cannot be null");
    }

    @Test
    void createOrder_quantityZeroInValidateItems() {
        CreateOrderRequest req = baseReq(List.of(item(5L,0)));
        when(addressRepository.findById(address.getId())).thenReturn(Optional.of(address));
        assertThatThrownBy(() -> orderService.createOrder(user, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be > 0");
    }

    @Test
    void createOrder_productMissing() {
        CreateOrderRequest req = baseReq(List.of(item(99L,1)));
        when(addressRepository.findById(address.getId())).thenReturn(Optional.of(address));
        when(productRepository.findAllById(anyCollection())).thenReturn(Collections.emptyList());
        assertThatThrownBy(() -> orderService.createOrder(user, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product 99");
    }

    @Test
    void createOrder_insufficientStock() {
        CreateOrderRequest req = baseReq(List.of(item(5L,3)));
        when(addressRepository.findById(address.getId())).thenReturn(Optional.of(address));
        Product p = product(5L, new BigDecimal("12.34"), 2L);
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(p));
        when(inventoryRepository.decrementIfAvailable(5L,3)).thenReturn(false);
        assertThatThrownBy(() -> orderService.createOrder(user, req))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void createOrder_priceNull() {
        CreateOrderRequest req = baseReq(List.of(item(5L,1)));
        when(addressRepository.findById(address.getId())).thenReturn(Optional.of(address));
        Product p = product(5L, null, 10L);
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(p));
        when(inventoryRepository.decrementIfAvailable(5L,1)).thenReturn(true);
        assertThatThrownBy(() -> orderService.createOrder(user, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Price not set");
    }

    // -------- Positive path --------

    @Test
    void createOrder_success() {
        ItemRequest a = item(20L,2);
        ItemRequest b = item(10L,3); // out of order to test sorting
        CreateOrderRequest req = baseReq(List.of(a,b));

        when(addressRepository.findById(address.getId())).thenReturn(Optional.of(address));
        Product p10 = product(10L, new BigDecimal("100"), 100L);
        Product p20 = product(20L, new BigDecimal("5.5"), 200L);
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(p10,p20));
        when(inventoryRepository.decrementIfAvailable(10L,3)).thenReturn(true);
        when(inventoryRepository.decrementIfAvailable(20L,2)).thenReturn(true);

        ArgumentCaptor<LocalOrder> cap = ArgumentCaptor.forClass(LocalOrder.class);
        when(localOrderRepository.save(cap.capture())).thenAnswer(inv -> {
            LocalOrder o = inv.getArgument(0);
            long id=1;
            for (LocalOrderItems li : o.getItems()) li.setId(id++);
            return o;
        });

        when(modelMapper.map(any(LocalOrder.class), eq(OrderResponse.class)))
                .thenAnswer(inv -> new OrderResponse());
        when(modelMapper.map(eq(address), eq(AddressResponse.class)))
                .thenReturn(new AddressResponse());

        OrderResponse resp = orderService.createOrder(user, req);
        LocalOrder saved = cap.getValue();

        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getItems()).hasSize(2);
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("311.00"); // (100*3)+(5.50*2)
        assertThat(Duration.between(saved.getCreatedAt(), saved.getExpiresAt()).toMinutes()).isEqualTo(15);
        assertThat(resp.getItems()).hasSize(2);

        InOrder inOrder = inOrder(inventoryRepository);
        inOrder.verify(inventoryRepository).decrementIfAvailable(10L,3);
        inOrder.verify(inventoryRepository).decrementIfAvailable(20L,2);
    }

    // -------- Helpers --------
    private CreateOrderRequest baseReq(List<ItemRequest> items) {
        CreateOrderRequest r = new CreateOrderRequest();
        r.setAddressId(address.getId());
        r.setItems(items);
        return r;
    }

    private ItemRequest item(Long productId, int qty) {
        ItemRequest ir = new ItemRequest();
        ir.setProductId(productId);
        ir.setQuantity(qty);
        return ir;
    }

    private Product product(Long id, BigDecimal price, Long stock) {
        Product p = new Product();
        p.setId(id);
        p.setPrice(price);
        Inventory inv = new Inventory();
        inv.setQuantity(stock);
        p.setInventory(inv);
        return p;
    }
}