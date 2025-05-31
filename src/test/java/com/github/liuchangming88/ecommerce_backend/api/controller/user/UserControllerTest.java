package com.github.liuchangming88.ecommerce_backend.api.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.liuchangming88.ecommerce_backend.api.model.AddressUpdateRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.LoginRequest;
import com.github.liuchangming88.ecommerce_backend.exception.DuplicatedAddressException;
import com.github.liuchangming88.ecommerce_backend.service.UserService;
import com.github.liuchangming88.ecommerce_backend.util.TestDataUtil;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Test
    void getProfile_authenticatedUser_returns200() throws Exception {
        // This user is present in the H2 test database
        LoginRequest userALoginRequest = TestDataUtil.createUserALoginRequest();
        String jwtToken = userService.loginUser(userALoginRequest);

        mockMvc.perform(MockMvcRequestBuilders.get("/users/profile")
                        .header("Authorization", "Bearer " + jwtToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(userALoginRequest.getUsername()));
    }

    @Test
    void getProfile_unauthenticatedUser_returns401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/users/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAddresses_authenticatedUserWithAddresses_returns200AndAddressList() throws Exception {
        // This user is present in the H2 test database
        LoginRequest userALoginRequest = TestDataUtil.createUserALoginRequest();
        String jwtToken = userService.loginUser(userALoginRequest);

        mockMvc.perform(MockMvcRequestBuilders.get("/users/addresses")
                        .header("Authorization", "Bearer " + jwtToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAddresses_authenticatedUserWithNoAddresses_returns200AndEmptyList() throws Exception {
        // This user is present in the H2 test database
        LoginRequest userDLoginRequest = TestDataUtil.createUserDLoginRequest();
        String jwtToken = userService.loginUser(userDLoginRequest);

        mockMvc.perform(MockMvcRequestBuilders.get("/users/addresses")
                        .header("Authorization", "Bearer " + jwtToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void addAddress_validRequest_returns201AndAddressResponse() throws Exception {
        AddressUpdateRequest addressUpdateRequest = new AddressUpdateRequest();
        addressUpdateRequest.setAddressLine1("123 Main St");
        addressUpdateRequest.setAddressLine2("Apt 4B");
        addressUpdateRequest.setCity("Metropolis");
        addressUpdateRequest.setCountry("USA");

        // This user is present in the H2 test database
        LoginRequest userALoginRequest = TestDataUtil.createUserALoginRequest();
        String jwtToken = userService.loginUser(userALoginRequest);

        mockMvc.perform(MockMvcRequestBuilders.post("/users/addresses")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addressUpdateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.addressLine1").value("123 Main St"))
                .andExpect(jsonPath("$.city").value("Metropolis"));
    }

    @Test
    void addAddress_duplicateAddress_returns409() throws Exception {
        AddressUpdateRequest addressUpdateRequest = new AddressUpdateRequest();
        addressUpdateRequest.setAddressLine1("123 Main St");
        addressUpdateRequest.setAddressLine2("Apt 4B");
        addressUpdateRequest.setCity("Metropolis");
        addressUpdateRequest.setCountry("USA");

        // This user is present in the H2 test database
        LoginRequest userALoginRequest = TestDataUtil.createUserALoginRequest();
        String jwtToken = userService.loginUser(userALoginRequest);

        mockMvc.perform(MockMvcRequestBuilders.post("/users/addresses")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addressUpdateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.addressLine1").value("123 Main St"))
                .andExpect(jsonPath("$.city").value("Metropolis"));

        mockMvc.perform(MockMvcRequestBuilders.post("/users/addresses")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addressUpdateRequest)))
                .andExpect(status().isConflict())
                .andExpect(result -> assertInstanceOf(DuplicatedAddressException.class, result.getResolvedException()));
    }

    @Test
    void updateAddress_validRequest_returns200AndUpdatedAddress() throws Exception {
        // Prepare the updated address details
        AddressUpdateRequest updateRequest = new AddressUpdateRequest();
        updateRequest.setAddressLine1("456 Elm St");
        updateRequest.setAddressLine2("Suite 5C");
        updateRequest.setCity("Gotham");
        updateRequest.setCountry("USA");

        // Authenticate userA
        LoginRequest userALoginRequest = TestDataUtil.createUserALoginRequest();
        String jwtToken = userService.loginUser(userALoginRequest);

        // Assume address with ID 1L exists for userA
        Long addressId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.put("/users/addresses/{id}", addressId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressLine1").value("456 Elm St"))
                .andExpect(jsonPath("$.city").value("Gotham"));
    }

    @Test
    void updateAddress_addressNotOwnedByUser_returns403() throws Exception {
        // Prepare the updated address details
        AddressUpdateRequest updateRequest = new AddressUpdateRequest();
        updateRequest.setAddressLine1("789 Oak St");
        updateRequest.setAddressLine2("Apt 9D");
        updateRequest.setCity("Star City");
        updateRequest.setCountry("USA");

        // Authenticate userD
        LoginRequest userDLoginRequest = TestDataUtil.createUserDLoginRequest();
        String jwtToken = userService.loginUser(userDLoginRequest);

        // Assume address with ID 1L is owned by userA, not userD
        Long addressId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.put("/users/addresses/{id}", addressId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(result -> assertInstanceOf(AccessDeniedException.class, result.getResolvedException()));
    }

    @Test
    void deleteAddress_validRequest_returns204() throws Exception {
        // Authenticate userA
        LoginRequest userALoginRequest = TestDataUtil.createUserALoginRequest();
        String jwtToken = userService.loginUser(userALoginRequest);

        // Assume address with ID 1L exists for userA
        Long addressId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.delete("/users/addresses/{id}", addressId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAddress_addressNotOwnedByUser_returns403() throws Exception {
        // Authenticate userD
        LoginRequest userDLoginRequest = TestDataUtil.createUserDLoginRequest();
        String jwtToken = userService.loginUser(userDLoginRequest);

        // Assume address with ID 1L is owned by userA, not userD
        Long addressId = 1L;

        mockMvc.perform(MockMvcRequestBuilders.delete("/users/addresses/{id}", addressId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isForbidden())
                .andExpect(result -> assertInstanceOf(AccessDeniedException.class, result.getResolvedException()));
    }
}
