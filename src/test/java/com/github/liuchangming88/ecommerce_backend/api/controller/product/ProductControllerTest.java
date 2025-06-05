package com.github.liuchangming88.ecommerce_backend.api.controller.product;

import com.github.liuchangming88.ecommerce_backend.api.model.LoginRequest;
import com.github.liuchangming88.ecommerce_backend.util.TestDataUtil;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void getAllProducts_returns200() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("/products")
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].name").value("Product #1"));
    }

    @Test
    void getProduct_returns200AndProduct() throws Exception {
        // Product that needs to be looked up
        Long productId = 1L;
        mockMvc.perform(
                MockMvcRequestBuilders.get("/products/{productId}", productId)
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Product #1"));
    }
}
