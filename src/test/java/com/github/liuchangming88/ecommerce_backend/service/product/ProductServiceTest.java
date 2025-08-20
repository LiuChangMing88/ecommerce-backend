package com.github.liuchangming88.ecommerce_backend.service.product;

import com.github.liuchangming88.ecommerce_backend.api.model.ProductResponse;
import com.github.liuchangming88.ecommerce_backend.configuration.MapperConfig;
import com.github.liuchangming88.ecommerce_backend.exception.ResourceNotFoundException;
import com.github.liuchangming88.ecommerce_backend.model.product.Inventory;
import com.github.liuchangming88.ecommerce_backend.model.product.Product;
import com.github.liuchangming88.ecommerce_backend.model.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @BeforeEach
    public void setUp() {
        // Since this mapper has custom mappings, use the real one to preserve configurations
        ModelMapper modelMapper = new MapperConfig().modelMapper();
        ReflectionTestUtils.setField(productService, "modelMapper", modelMapper);
    }

    @Test
    public void getAllProducts_returnsMappedResponses() {
        // Arrange
        Product product1 = new Product();
        product1.setName("Product #1");

        Product product2 = new Product();
        product2.setName("Product #2");

        Page<Product> products = new PageImpl<>(List.of(product1, product2));

        when(productRepository.findAll(any(Pageable.class))).thenReturn(products);

        // Act
        Page<ProductResponse> result = productService.getAllProducts(0, 10);

        // Assert
        assertEquals(2, result.getContent().size());
        assertEquals(product1.getName(), result.getContent().get(0).getName());
        assertEquals(product2.getName(), result.getContent().get(1).getName());
    }

    @Test
    public void getProduct_productDoesNotExist_throwsResourceNotFoundException() {
        // Act and assert
        assertThrows(ResourceNotFoundException.class,
                () -> productService.getProduct(1L));
    }

    @Test
    public void getProduct_productExists_returnsProduct() {
        // Arrange
        Product product = new Product();
        Inventory inventory = new Inventory();

        product.setId(1L);
        product.setName("Test Product");
        product.setShortDescription("Short description");
        product.setLongDescription("Long description");
        product.setPrice(BigDecimal.valueOf(8.88));
        inventory.setId(1L);
        inventory.setQuantity(5L);
        inventory.setProduct(product);
        product.setInventory(inventory);

        // Arrange
        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        // Act
        ProductResponse response = productService.getProduct(product.getId());

        // Assert
        assertEquals(product.getName(), response.getName());
        assertEquals(product.getInventory().getQuantity(), response.getQuantity());
    }
}
