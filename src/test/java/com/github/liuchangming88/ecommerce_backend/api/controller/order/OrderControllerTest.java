package com.github.liuchangming88.ecommerce_backend.api.controller.order;

import com.github.liuchangming88.ecommerce_backend.model.user.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.user.repository.LocalUserRepository;
import com.github.liuchangming88.ecommerce_backend.service.infrastructure.JwtService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class OrderControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalUserRepository localUserRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    public void getAllOrders_unauthenticated_returns401() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("/orders")
        ).andExpect(status().isUnauthorized());
    }

    // Test that the endpoint should return the authenticated user's list (not another user's)
    @Test
    public void getAllOrders_authenticated_returnsAuthenticatedUsersOrders() throws Exception {
        // This user is verified and already present in the test database
        testGetAllOrdersReturnsCorrectUsersOrders("usernameA", 3);

        testGetAllOrdersReturnsCorrectUsersOrders("usernameC", 2);
    }

    private void testGetAllOrdersReturnsCorrectUsersOrders(String username, Integer expectedNumberOfOrders) throws Exception {
        LocalUser user = localUserRepository.findByUsernameIgnoreCase(username).get();
        String userToken = jwtService.generateJwt(user);
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/orders")
                                .header("Authorization", "Bearer " + userToken)
                ).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(expectedNumberOfOrders));
    }

}
