package com.github.liuchangming88.ecommerce_backend.service;

import com.github.liuchangming88.ecommerce_backend.api.model.ProductResponse;
import com.github.liuchangming88.ecommerce_backend.model.Product;
import com.github.liuchangming88.ecommerce_backend.model.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ProductService productService;

    @Test
    public void getAllProducts_returnsMappedResponses() {
        // Arrange
        Product product1 = new Product();
        Product product2 = new Product();
        List<Product> products = List.of(product1, product2);

        ProductResponse response1 = new ProductResponse();
        ProductResponse response2 = new ProductResponse();

        when(productRepository.findAll()).thenReturn(products);
        when(modelMapper.map(product1, ProductResponse.class)).thenReturn(response1);
        when(modelMapper.map(product2, ProductResponse.class)).thenReturn(response2);

        // Act
        List<ProductResponse> result = productService.getAllProducts();

        // Assert
        assertEquals(2, result.size());
        assertEquals(response1, result.get(0));
        assertEquals(response2, result.get(1));
    }
}
