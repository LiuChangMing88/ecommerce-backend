package com.github.liuchangming88.ecommerce_backend.configuration;

import com.github.liuchangming88.ecommerce_backend.api.model.OrderItemsResponse;
import com.github.liuchangming88.ecommerce_backend.api.model.ProductResponse;
import com.github.liuchangming88.ecommerce_backend.model.order.LocalOrderItems;
import com.github.liuchangming88.ecommerce_backend.model.product.Product;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class MapperConfig {
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        // Use strict matching so you don’t accidentally map wrong properties
        mapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(Configuration.AccessLevel.PRIVATE)
                .setMatchingStrategy(MatchingStrategies.STRICT);

        // Used in ProductService to map Product into ProductResponseBody
        // Tell ModelMapper how to map nested Inventory.quantity → ProductResponseBody.quantity
        mapper.createTypeMap(Product.class, ProductResponse.class)
                .addMapping(src -> src.getInventory().getQuantity(),
                        ProductResponse::setQuantity);

        // Used in OrderService to map LocalOrder into OrderResponseBody
        mapper.createTypeMap(LocalOrderItems.class, OrderItemsResponse.class)
                .addMapping(src -> src.getProduct().getId(),
                        OrderItemsResponse::setProductId)
                .addMapping(src -> src.getProduct().getName(),
                        OrderItemsResponse::setProductName);

        return mapper;
    }
}
