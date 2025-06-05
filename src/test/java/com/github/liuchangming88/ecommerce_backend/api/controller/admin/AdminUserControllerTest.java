package com.github.liuchangming88.ecommerce_backend.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.liuchangming88.ecommerce_backend.api.model.AddressUpdateRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.LoginRequest;
import com.github.liuchangming88.ecommerce_backend.service.UserService;
import com.github.liuchangming88.ecommerce_backend.util.TestDataUtil;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AdminUserControllerTest {
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
    void getProfile_adminUser_returns200AndProfile() throws Exception {
        // Admin user
        LoginRequest userELoginRequest = TestDataUtil.createUserELoginRequest();
        String jwtToken = obtainJwtToken(userELoginRequest);

        // User that needs to be looked up
        LoginRequest userALoginRequest = TestDataUtil.createUserALoginRequest();
        Long targetUserId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.get("/admins/users/{userId}/profile", targetUserId)
                        .header("Authorization", "Bearer " + jwtToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(userALoginRequest.getUsername()));
    }

    @Test
    void getAddresses_adminUser_returns200AndAddressList() throws Exception {
        // Admin user
        LoginRequest adminLoginRequest = TestDataUtil.createUserELoginRequest();
        String jwtToken = obtainJwtToken(adminLoginRequest);

        // User A
        Long targetUserId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.get("/admins/users/{userId}/addresses", targetUserId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void addAddress_adminUser_returns201AndAddressResponse() throws Exception {
        // Admin user
        LoginRequest adminLoginRequest = TestDataUtil.createUserELoginRequest();
        String jwtToken = obtainJwtToken(adminLoginRequest);

        // User A
        Long targetUserId = 1L;

        AddressUpdateRequest addressUpdateRequest = new AddressUpdateRequest();
        addressUpdateRequest.setAddressLine1("456 Elm St");
        addressUpdateRequest.setAddressLine2("Suite 5C");
        addressUpdateRequest.setCity("Gotham");
        addressUpdateRequest.setCountry("USA");

        mockMvc.perform(MockMvcRequestBuilders.post("/admins/users/{userId}/addresses", targetUserId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addressUpdateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.addressLine1").value("456 Elm St"))
                .andExpect(jsonPath("$.city").value("Gotham"));
    }

    @Test
    void updateAddress_adminUser_returns200AndUpdatedAddress() throws Exception {
        // Admin user
        LoginRequest adminLoginRequest = TestDataUtil.createUserELoginRequest();
        String jwtToken = obtainJwtToken(adminLoginRequest);

        // User A and user A's address
        Long targetUserId = 1L;
        Long addressId = 1L;

        AddressUpdateRequest updateRequest = new AddressUpdateRequest();
        updateRequest.setAddressLine1("789 Oak St");
        updateRequest.setAddressLine2("Apt 9D");
        updateRequest.setCity("Star City");
        updateRequest.setCountry("USA");

        mockMvc.perform(MockMvcRequestBuilders.put("/admins/users/{userId}/addresses/{addressId}", targetUserId, addressId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressLine1").value("789 Oak St"))
                .andExpect(jsonPath("$.city").value("Star City"));
    }

    @Test
    void deleteAddress_adminUser_returns204() throws Exception {
        // Admin user
        LoginRequest adminLoginRequest = TestDataUtil.createUserELoginRequest();
        String jwtToken = obtainJwtToken(adminLoginRequest);

        // User A and user A's address
        Long targetUserId = 1L;
        Long addressId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.delete("/admins/users/{userId}/addresses/{addressId}", targetUserId, addressId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void getProfile_normalUser_returns403() throws Exception {
        // Normal user
        LoginRequest userALoginRequest = TestDataUtil.createUserALoginRequest();
        String jwtToken = obtainJwtToken(userALoginRequest);

        mockMvc.perform(MockMvcRequestBuilders.get("/admins/users/{userId}/profile", 1L)
                        .header("Authorization", "Bearer " + jwtToken)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void getProfile_unauthenticatedUser_returns401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admins/users/{userId}/profile", 1L))
                .andExpect(status().isUnauthorized());
    }
}
