package com.github.liuchangming88.ecommerce_backend.api.controller.product;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

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
        ).andExpect(status().isOk());
    }
}
