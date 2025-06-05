package com.github.liuchangming88.ecommerce_backend.service;

import com.github.liuchangming88.ecommerce_backend.api.model.ProductRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.ProductResponse;
import com.github.liuchangming88.ecommerce_backend.configuration.MapperConfig;
import com.github.liuchangming88.ecommerce_backend.exception.DuplicateResourceException;
import com.github.liuchangming88.ecommerce_backend.exception.ResourceNotFoundException;
import com.github.liuchangming88.ecommerce_backend.model.Inventory;
import com.github.liuchangming88.ecommerce_backend.model.Product;
import com.github.liuchangming88.ecommerce_backend.model.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminProductServiceTest {
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private AdminProductService adminProductService;

    @BeforeEach
    void setUp() {
        // Since this mapper has custom mappings, use the real one to preserve configurations
        ModelMapper modelMapper = new MapperConfig().modelMapper();
        ReflectionTestUtils.setField(adminProductService, "modelMapper", modelMapper);
    }

    @Test
    void createProduct_productAlreadyExists_throwsDuplicateResourceException() {
        // Arrange
        ProductRequest request = new ProductRequest();
        request.setName("Sample Product");

        when(productRepository.existsByName(request.getName())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> adminProductService.createProduct(request));

        verify(productRepository).existsByName(request.getName());
        verify(productRepository, times(0)).save(any(Product.class));
    }

    @Test
    void createProduct_newProduct_returnsProductResponse() {
        // Arrange
        ProductRequest request = new ProductRequest();
        request.setName("New Product");
        request.setShortDescription("Short description");
        request.setLongDescription("Detailed description");
        request.setPrice(99.99);
        request.setQuantity(100L);

        Product product = mapRequestToProduct(request);

        // Stub
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // Act
        ProductResponse response = adminProductService.createProduct(request);

        // Assert
        assertNotNull(response);
        assertEquals(request.getName(), response.getName());
        assertEquals(request.getShortDescription(), response.getShortDescription());
        assertEquals(request.getLongDescription(), response.getLongDescription());
        assertEquals(request.getPrice(), response.getPrice());
        assertEquals(request.getQuantity(), response.getQuantity());

        // Verify
        verify(productRepository).existsByName(request.getName());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void updateProduct_productNotFound_throwsResourceNotFoundException() {
        // Arrange
        Long productId = 1L;
        ProductRequest request = new ProductRequest();
        request.setName("Updated Product");

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> adminProductService.updateProduct(productId, request));

        verify(productRepository).findById(productId);
        verify(productRepository, times(0)).save(any(Product.class));
    }

    @Test
    void updateProduct_nameAlreadyExists_throwsDuplicateResourceException() {
        // Arrange
        Long productId = 1L;

        Product existingProduct = new Product();
        existingProduct.setId(productId);
        existingProduct.setName("Original Product");

        ProductRequest request = new ProductRequest();
        request.setName("Updated Product");
        request.setShortDescription("Short");
        request.setLongDescription("Long");
        request.setPrice(10.0);
        request.setQuantity(50L);

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(productRepository.existsByName(request.getName())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> adminProductService.updateProduct(productId, request));

        verify(productRepository).findById(productId);
        verify(productRepository).existsByName(request.getName());
        verify(productRepository, times(0)).save(any(Product.class));
    }

    @Test
    void updateProduct_validUpdate_returnsProductResponse() {
        // Arrange
        Long productId = 1L;

        ProductRequest oldProduct = new ProductRequest();
        oldProduct.setName("Old product");
        oldProduct.setShortDescription("Old short");
        oldProduct.setLongDescription("Old long");
        oldProduct.setPrice(55.0);
        oldProduct.setQuantity(20L);

        Product existingProduct = mapRequestToProduct(oldProduct);

        ProductRequest request = new ProductRequest();
        request.setName("Updated Product");
        request.setShortDescription("Updated short");
        request.setLongDescription("Updated long");
        request.setPrice(75.0);
        request.setQuantity(40L);

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(productRepository.existsByName(request.getName())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductResponse response = adminProductService.updateProduct(productId, request);

        // Assert
        assertNotNull(response);
        assertEquals(request.getName(), response.getName());
        assertEquals(request.getShortDescription(), response.getShortDescription());
        assertEquals(request.getLongDescription(), response.getLongDescription());
        assertEquals(request.getPrice(), response.getPrice());
        assertEquals(request.getQuantity(), response.getQuantity());

        // Verify
        verify(productRepository).findById(productId);
        verify(productRepository).existsByName(request.getName());
        verify(productRepository).save(existingProduct);
    }

    @Test
    void deleteProduct_productNotFound_throwsResourceNotFoundException() {
        // Arrange
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> adminProductService.deleteProduct(productId));

        // Verify
        verify(productRepository).findById(productId);
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void deleteProduct_existingProduct_deletesProduct() {
        // Arrange
        Long productId = 1L;
        Product product = new Product();
        product.setId(productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // Act
        adminProductService.deleteProduct(productId);

        // Verify
        verify(productRepository).findById(productId);
        verify(productRepository).delete(product);
    }

    @Test
    void updateProductQuantity_productNotFound_throwsResourceNotFoundException() {
        // Arrange
        Long productId = 1L;
        Long quantity = 50L;

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> adminProductService.updateProductQuantity(productId, quantity));

        // Verify
        verify(productRepository).findById(productId);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProductQuantity_existingProduct_updatesQuantity() {
        // Arrange
        Long productId = 1L;
        Long newQuantity = 75L;

        ProductRequest request = new ProductRequest();
        request.setName("Sample Product");
        request.setShortDescription("Short description");
        request.setLongDescription("Long description");
        request.setPrice(99.99);
        request.setQuantity(50L);

        Product product = mapRequestToProduct(request);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        adminProductService.updateProductQuantity(productId, newQuantity);

        // Assert
        assertEquals(newQuantity, product.getInventory().getQuantity());

        // Verify
        verify(productRepository).findById(productId);
        verify(productRepository).save(product);
    }

    private static Product mapRequestToProduct(ProductRequest request) {
        Product product = new Product();
        product.setId(1L);
        product.setName(request.getName());
        product.setLongDescription(request.getLongDescription());
        product.setShortDescription(request.getShortDescription());
        product.setPrice(request.getPrice());
        Inventory inventory = new Inventory();
        inventory.setId(1L);
        inventory.setProduct(product);
        inventory.setQuantity(request.getQuantity());
        product.setInventory(inventory);
        return product;
    }
}
