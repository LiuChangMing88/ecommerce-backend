package com.github.liuchangming88.ecommerce_backend.api.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.liuchangming88.ecommerce_backend.api.model.LoginRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.RegistrationRequest;
import com.github.liuchangming88.ecommerce_backend.model.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.VerificationToken;
import com.github.liuchangming88.ecommerce_backend.model.repository.LocalUserRepository;
import com.github.liuchangming88.ecommerce_backend.model.repository.VerificationTokenRepository;
import com.github.liuchangming88.ecommerce_backend.util.TestDataUtil;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthenticationControllerTest {
    // This is here so the tests can connect to a dummy mail sender.
    // Without this, tests will throw a 500 INTERNAL_SERVER_ERROR status because they can't connect to a mail sending service.
    @RegisterExtension
    private final static GreenMailExtension greenMailExtension = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("springboot", "secret"))
            .withPerMethodLifecycle(true);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private LocalUserRepository localUserRepository;

    @Test
    void registerUser_userAlreadyExists_returns409() throws Exception {
        // This user already exists
        RegistrationRequest registrationRequest = TestDataUtil.createUserARegisterRequest();

        mockMvc.perform(
                MockMvcRequestBuilders.post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest))
        ).andExpect(status().isConflict());
    }

    @Test
    void registerUser_newCredentials_returns200() throws Exception {
        // This user doesn't exist
        RegistrationRequest registrationRequest = TestDataUtil.createTestRegisterRequest();

        mockMvc.perform(
                MockMvcRequestBuilders.post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest))
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(registrationRequest.getUsername()))
                .andExpect(jsonPath("$.email").value(registrationRequest.getEmail()));
    }

    @Test
    void loginUser_wrongCredentials_returns400() throws Exception {
        LoginRequest loginRequest = TestDataUtil.createUserALoginRequest();
        loginRequest.setPassword("Gibberish");

        mockMvc.perform(
                MockMvcRequestBuilders.post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void loginUser_correctCredentialsUnverified_returns403() throws Exception {
        LoginRequest loginRequest = TestDataUtil.createUserBLoginRequest();

        mockMvc.perform(
                MockMvcRequestBuilders.post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        ).andExpect(status().isForbidden());
    }

    @Test
    void loginUser_correctCredentialsVerified_returns200() throws Exception {
        LoginRequest loginRequest = TestDataUtil.createUserALoginRequest();

        mockMvc.perform(
                MockMvcRequestBuilders.post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt").isString());
    }

    @Test
    void verifyUser_invalidToken_returns400() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("/auth/verify")
                        .param("token", "Gibberish")
        ).andExpect(status().isBadRequest());
    }

    @Test
    void verifyUser_correctToken_returns200() throws Exception {
        // Login user to have a verification token sent to GreenMail
        LoginRequest loginRequest = TestDataUtil.createUserBLoginRequest();
        mockMvc.perform(
                MockMvcRequestBuilders.post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        );

        // Extract the token from greenmail
        String mailMessage = greenMailExtension.getReceivedMessages()[0].getContent().toString();
        int tokenIndex = mailMessage.indexOf("token=") + "token=".length();
        String token = mailMessage.substring(tokenIndex).trim();

        mockMvc.perform(
                MockMvcRequestBuilders.get("/auth/verify")
                        .param("token", token)
        ).andExpect(status().isOk())
                .andExpect(content().string("User has been verified!"));
    }

    @Test
    void verifyUser_expiredToken_returns400() throws Exception {
        // Retrieve user B
        Optional<LocalUser> opUser = localUserRepository.findByUsernameIgnoreCase("usernameB");
        LocalUser user = opUser.get();

        // Create an expired verification token
        VerificationToken expiredToken = new VerificationToken();
        expiredToken.setToken("expired-token");
        expiredToken.setLocalUser(user);
        expiredToken.setExpireAt(LocalDateTime.now());
        verificationTokenRepository.save(expiredToken);

        // Perform the verification request with the expired token
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/auth/verify")
                                .param("token", "expired-token")
                ).andExpect(status().isBadRequest())
                .andExpect(status().isBadRequest());
    }
}
