package com.github.liuchangming88.ecommerce_backend.api.security;

import com.github.liuchangming88.ecommerce_backend.model.user.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.user.repository.LocalUserRepository;
import com.github.liuchangming88.ecommerce_backend.service.infrastructure.JwtService;
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
public class JwtRequestFilterTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private LocalUserRepository localUserRepository;

    // Only authenticated users can access this
    private final String AUTHENTICATION_PATH = "/users/profile";

    // Test that unauthenticated requests are rejected
    @Test
    public void unauthenticatedRequests_rejected() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(AUTHENTICATION_PATH)
        ).andExpect(status().isUnauthorized());
    }

    // Test that invalid tokens are rejected
    @Test
    public void invalidTokenRequests_rejected() throws Exception {
        // Null token header
        mockMvc.perform(
                MockMvcRequestBuilders.get(AUTHENTICATION_PATH)
                        .header("Authorization", "")
        ).andExpect(status().isUnauthorized());

        // Token header is invalid
        mockMvc.perform(
                MockMvcRequestBuilders.get(AUTHENTICATION_PATH)
                        .header("Authorization", "Gibberish")
        ).andExpect(status().isUnauthorized());

        // Token header is valid but the jwt token is invalid
        mockMvc.perform(
                MockMvcRequestBuilders.get(AUTHENTICATION_PATH)
                        .header("Authorization", "Bearer Gibberish")
        ).andExpect(status().isUnauthorized());
    }

    // Test that unverified users that somehow got a valid token are rejected
    @Test
    public void unverifiedButValidTokenRequests_rejected() throws Exception {
        // This user is unverified (already present in the database)
        LocalUser unverifiedUser = localUserRepository.findByUsernameIgnoreCase("usernameB").get();
        String jwtToken = jwtService.generateJwt(unverifiedUser);

        mockMvc.perform(
                MockMvcRequestBuilders.get(AUTHENTICATION_PATH)
                        .header("Authorization", "Bearer " + jwtToken)
        ).andExpect(status().isForbidden());
    }

    // Test that verified users with correct, valid tokens are authenticated
    @Test
    public void verifiedAndValidTokenRequests_authenticated() throws Exception {
        // This user is verified (already present in the database)
        LocalUser unverifiedUser = localUserRepository.findByUsernameIgnoreCase("usernameA").get();
        String jwtToken = jwtService.generateJwt(unverifiedUser);

        mockMvc.perform(
                MockMvcRequestBuilders.get(AUTHENTICATION_PATH)
                        .header("Authorization", "Bearer " + jwtToken)
        ).andExpect(status().isOk());
    }
}
