package com.github.liuchangming88.ecommerce_backend.service;

import com.github.liuchangming88.ecommerce_backend.api.model.AddressResponse;
import com.github.liuchangming88.ecommerce_backend.api.model.OrderQuantitiesResponse;
import com.github.liuchangming88.ecommerce_backend.api.model.OrderResponse;
import com.github.liuchangming88.ecommerce_backend.model.LocalOrder;
import com.github.liuchangming88.ecommerce_backend.model.repository.LocalOrderRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {
    LocalOrderRepository localOrderRepository;
    ModelMapper modelMapper;

    public OrderService(LocalOrderRepository localOrderRepository, ModelMapper modelMapper) {
        this.localOrderRepository = localOrderRepository;
        this.modelMapper = modelMapper;
    }

    public List<OrderResponse> getAllOrders(Long userId) {
        // Get all the user's orders
        List<LocalOrder> localOrderList = localOrderRepository.findByLocalUser_Id(userId);

        // Turn it into dto and then returns to the client
        return localOrderList.stream()
                .map(orderList -> {
                    // Map LocalOrderQuantities into its dto
                    List<OrderQuantitiesResponse> orderQuantitiesResponseList = orderList.getQuantities().stream()
                            .map(q -> modelMapper.map(q, OrderQuantitiesResponse.class)).toList();

                    // Map Address into its dto
                    AddressResponse addressResponse = modelMapper.map(orderList.getAddress(), AddressResponse.class);

                    // Map the final LocalOrder into its dto and then return it to the list of OrderResponseBody
                    OrderResponse orderResponse = modelMapper.map(orderList, OrderResponse.class);
                    orderResponse.setOrderQuantitiesResponseList(orderQuantitiesResponseList);
                    orderResponse.setAddressResponse(addressResponse);
                    return orderResponse;
                }).toList();
    }
}
