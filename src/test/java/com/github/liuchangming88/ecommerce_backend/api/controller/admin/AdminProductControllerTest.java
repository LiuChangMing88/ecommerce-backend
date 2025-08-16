package com.github.liuchangming88.ecommerce_backend.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.liuchangming88.ecommerce_backend.api.model.LoginRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.ProductQuantityUpdateRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.ProductRequest;
import com.github.liuchangming88.ecommerce_backend.exception.ResourceNotFoundException;
import com.github.liuchangming88.ecommerce_backend.model.product.Inventory;
import com.github.liuchangming88.ecommerce_backend.model.product.Product;
import com.github.liuchangming88.ecommerce_backend.service.user.UserService;
import com.github.liuchangming88.ecommerce_backend.util.TestDataUtil;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AdminProductControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    // Helper method to obtain JWT token for a given login request
    private String obtainJwtToken(LoginRequest loginRequest) {
        return userService.loginUser(loginRequest);
    }

    @Test
    void createProduct_adminUser_returns201AndProductResponse() throws Exception {
        // Admin user
        LoginRequest adminLoginRequest = TestDataUtil.createUserELoginRequest();
        String jwtToken = obtainJwtToken(adminLoginRequest);

        ProductRequest productRequest = createProductRequest();

        mockMvc.perform(MockMvcRequestBuilders.post("/admins/products")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(productRequest.getName()))
                .andExpect(jsonPath("$.price").value(productRequest.getPrice()))
                .andExpect(jsonPath("$.quantity").value(productRequest.getQuantity()));
    }

    @Test
    void updateProduct_adminUser_returns200AndUpdatedProductResponse() throws Exception {
        // Admin user
        LoginRequest adminLoginRequest = TestDataUtil.createUserELoginRequest();
        String jwtToken = obtainJwtToken(adminLoginRequest);

        // This product is present in the database
        Product product = createTestProductA();

        // This request creates a product that is not yet present in the database
        ProductRequest updateRequest = createProductRequest();

        mockMvc.perform(MockMvcRequestBuilders.put("/admins/products/{productId}", product.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(updateRequest.getName()))
                .andExpect(jsonPath("$.price").value(updateRequest.getPrice()))
                .andExpect(jsonPath("$.quantity").value(updateRequest.getQuantity()));
    }

    @Test
    void deleteProduct_adminUser_returns204() throws Exception {
        // Admin user
        LoginRequest adminLoginRequest = TestDataUtil.createUserELoginRequest();
        String jwtToken = obtainJwtToken(adminLoginRequest);

        // Delete product 1
        Long productId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.delete("/admins/products/{productId}", productId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        // Verify the product is deleted
        mockMvc.perform(MockMvcRequestBuilders.get("/products/{productId}", productId))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertInstanceOf(ResourceNotFoundException.class, result.getResolvedException()));
    }

    @Test
    void updateProductQuantity_adminUser_returns200() throws Exception {
        // Admin user
        LoginRequest adminLoginRequest = TestDataUtil.createUserELoginRequest();
        String jwtToken = obtainJwtToken(adminLoginRequest);

        // This product is present in the database
        Product product = createTestProductA();

        ProductQuantityUpdateRequest productQuantityUpdateRequest = new ProductQuantityUpdateRequest();
        productQuantityUpdateRequest.setQuantity(100L);

        mockMvc.perform(MockMvcRequestBuilders.patch("/admins/products/{productId}/quantity", product.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productQuantityUpdateRequest)))
                .andExpect(status().isOk());

        // Verify the quantity is updated
        mockMvc.perform(MockMvcRequestBuilders.get("/products/{productId}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(100));
    }

    @Test
    void createProduct_normalUser_returns403() throws Exception {
        // Normal user
        LoginRequest userALoginRequest = TestDataUtil.createUserALoginRequest();
        String jwtToken = obtainJwtToken(userALoginRequest);

        ProductRequest productRequest = createProductRequest();

        mockMvc.perform(MockMvcRequestBuilders.post("/admins/products")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_unauthenticatedUser_returns401() throws Exception {
        ProductRequest productRequest = createProductRequest();

        mockMvc.perform(MockMvcRequestBuilders.post("/admins/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isUnauthorized());
    }

    // This is product 1 in the test database
    static private Product createTestProductA() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Product #1");
        product.setShortDescription("Product one short description.");
        product.setLongDescription("This is a very long description of product #1.");
        product.setPrice(BigDecimal.valueOf(5.50));
        Inventory inventory = new Inventory();
        inventory.setQuantity(5L);
        inventory.setId(1L);
        product.setInventory(inventory);
        return product;
    }

    // This is a test product that is not present in the database
    static private Product createTestProduct() {
        Product product = new Product();
        product.setName("Test Product");
        product.setShortDescription("Test product short description.");
        product.setLongDescription("Test product long description.");
        product.setPrice(BigDecimal.valueOf(8.88));
        Inventory inventory = new Inventory();
        inventory.setQuantity(10L);
        product.setInventory(inventory);
        return product;
    }

    // This is a product creation request that is identical to the test product above
    static private ProductRequest createProductRequest() {
        ProductRequest productRequest = new ProductRequest();
        productRequest.setName("Test Product");
        productRequest.setShortDescription("Test product short description.");
        productRequest.setLongDescription("Test product long description.");
        productRequest.setPrice(BigDecimal.valueOf(8.88));
        productRequest.setQuantity(10L);
        return productRequest;
    }
}
