package com.github.liuchangming88.ecommerce_backend.service;

import com.github.liuchangming88.ecommerce_backend.api.model.AddressResponse;
import com.github.liuchangming88.ecommerce_backend.api.model.OrderItemsResponse;
import com.github.liuchangming88.ecommerce_backend.api.model.OrderResponse;
import com.github.liuchangming88.ecommerce_backend.model.Address;
import com.github.liuchangming88.ecommerce_backend.model.LocalOrder;
import com.github.liuchangming88.ecommerce_backend.model.LocalOrderItems;
import com.github.liuchangming88.ecommerce_backend.model.repository.LocalOrderRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private LocalOrderRepository localOrderRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private OrderService orderService;

    @Test
    public void getAllOrders_withValidUserId_returnsMappedResponses() {
        Long userId = 1L;

        // Setup mock LocalOrder, quantities, and address
        LocalOrderItems quantity = new LocalOrderItems();
        List<LocalOrderItems> quantities = List.of(quantity);

        LocalOrder order = new LocalOrder();
        order.setItems(quantities);
        order.setAddress(new Address());
        List<LocalOrder> orderList = List.of(order);

        // Stubbing repository
        when(localOrderRepository.findByLocalUser_Id(userId)).thenReturn(orderList);

        // Mocked DTOs
        OrderItemsResponse quantityResponse = new OrderItemsResponse();
        AddressResponse addressResponse = new AddressResponse();
        OrderResponse orderResponse = new OrderResponse();

        // Stubbing mapping
        when(modelMapper.map(quantity, OrderItemsResponse.class)).thenReturn(quantityResponse);
        when(modelMapper.map(order.getAddress(), AddressResponse.class)).thenReturn(addressResponse);
        when(modelMapper.map(order, OrderResponse.class)).thenReturn(orderResponse);

        // Execute
        List<OrderResponse> result = orderService.getAllOrders(userId);

        // Verify
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(orderResponse, result.get(0));
        Assertions.assertEquals(List.of(quantityResponse), result.get(0).getItems());
        Assertions.assertEquals(addressResponse, result.get(0).getAddressResponse());

        verify(localOrderRepository, times(1)).findByLocalUser_Id(userId);
        verify(modelMapper, times(1)).map(quantity, OrderItemsResponse.class);
        verify(modelMapper, times(1)).map(order.getAddress(), AddressResponse.class);
        verify(modelMapper, times(1)).map(order, OrderResponse.class);
    }
}
