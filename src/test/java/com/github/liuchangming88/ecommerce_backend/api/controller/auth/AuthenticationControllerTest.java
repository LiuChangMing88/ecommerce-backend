package com.github.liuchangming88.ecommerce_backend.api.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.liuchangming88.ecommerce_backend.api.model.ForgotPasswordRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.LoginRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.RegistrationRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.ResetPasswordRequest;
import com.github.liuchangming88.ecommerce_backend.exception.*;
import com.github.liuchangming88.ecommerce_backend.model.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.PasswordResetToken;
import com.github.liuchangming88.ecommerce_backend.model.VerificationToken;
import com.github.liuchangming88.ecommerce_backend.model.repository.LocalUserRepository;
import com.github.liuchangming88.ecommerce_backend.model.repository.PasswordResetTokenRepository;
import com.github.liuchangming88.ecommerce_backend.model.repository.VerificationTokenRepository;
import com.github.liuchangming88.ecommerce_backend.service.EncryptionService;
import com.github.liuchangming88.ecommerce_backend.service.UserService;
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
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private LocalUserRepository localUserRepository;

    @Autowired
    private UserService userService;

    @Test
    void registerUser_userAlreadyExists_returns409() throws Exception {
        // This user already exists
        RegistrationRequest registrationRequest = TestDataUtil.createUserARegisterRequest();

        mockMvc.perform(
                MockMvcRequestBuilders.post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest))
        ).andExpect(status().isConflict())
                .andExpect(result -> assertInstanceOf(DuplicatedUserException.class, result.getResolvedException()));
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
        ).andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(IncorrectPasswordException.class, result.getResolvedException()));
    }

    @Test
    void loginUser_correctCredentialsUnverified_returns403() throws Exception {
        LoginRequest loginRequest = TestDataUtil.createUserBLoginRequest();

        mockMvc.perform(
                MockMvcRequestBuilders.post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        ).andExpect(status().isForbidden())
                .andExpect(result -> assertInstanceOf(UserNotVerifiedException.class, result.getResolvedException()));
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
    void verifyUser_invalidToken_returns401() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("/auth/verify")
                        .param("token", "Gibberish")
        ).andExpect(status().isUnauthorized())
                .andExpect(result -> assertInstanceOf(InvalidTokenException.class, result.getResolvedException()));
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
    void verifyUser_expiredToken_returns401() throws Exception {
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
                ).andExpect(status().isUnauthorized())
                .andExpect(result -> assertInstanceOf(TokenExpiredException.class, result.getResolvedException()));
    }

    @Test
    void forgotPassword_invalidEmail_returns400() throws Exception {
        // Create a request with an invalid email
        ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest();
        forgotPasswordRequest.setEmail("InvalidEmail");

        mockMvc.perform(
                MockMvcRequestBuilders.post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotPasswordRequest))
        ).andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException()));
    }

    @Test
    void forgotPassword_validEmailButDoesNotExist_returns200() throws Exception {
        // Create a request with a valid
        ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest();
        forgotPasswordRequest.setEmail("GibberishEmail@gmail.com");

        mockMvc.perform(
                MockMvcRequestBuilders.post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotPasswordRequest))
        ).andExpect(status().isOk());
    }

    @Test
    void forgotPassword_validAndExistingEmail_returns200() throws Exception {
        // Create a request with a valid and existing email (this email exists in the test database)
        ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest();
        forgotPasswordRequest.setEmail("emailA@gmail.com");

        mockMvc.perform(
                MockMvcRequestBuilders.post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotPasswordRequest))
        ).andExpect(status().isOk());
    }

    @Test
    void validateResetToken_invalidToken_returns401() throws Exception {
        String token = "InvalidToken";

        mockMvc.perform(
                MockMvcRequestBuilders.get("/auth/reset-password")
                        .param("token" , token)
        ).andExpect(status().isUnauthorized())
                .andExpect(result -> assertInstanceOf(InvalidTokenException.class, result.getResolvedException()));
    }

    @Test
    void validateResetToken_expiredToken_returns401() throws Exception {
        // This user is present in the database
        LocalUser userA = localUserRepository.findByUsernameIgnoreCase("usernameA").get();

        // Create an expired token
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setExpireAt(LocalDateTime.now().minusSeconds(1));
        passwordResetToken.setToken("ExpiredToken");
        passwordResetToken.setLocalUser(userA);
        passwordResetTokenRepository.save(passwordResetToken);

        // Perform
        mockMvc.perform(
                MockMvcRequestBuilders.get("/auth/reset-password")
                        .param("token" , passwordResetToken.getToken())
        ).andExpect(status().isUnauthorized())
                .andExpect(result -> assertInstanceOf(TokenExpiredException.class, result.getResolvedException()));
    }

    @Test
    void validateResetToken_validUnexpiredToken_returns200() throws Exception {
        // This user is present in the database
        LocalUser userA = localUserRepository.findByUsernameIgnoreCase("usernameA").get();

        // Create an unexpired token
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setExpireAt(LocalDateTime.now().plusSeconds(900));
        passwordResetToken.setToken("ValidUnexpiredToken");
        passwordResetToken.setLocalUser(userA);
        passwordResetTokenRepository.save(passwordResetToken);

        // Perform
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/auth/reset-password")
                                .param("token" , passwordResetToken.getToken())
                ).andExpect(status().isOk());
    }

    @Test
    void resetPassword_invalidPassword_returns400() throws Exception {
        ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest();
        // Password must be between 8-16 characters, has at least 1 uppercase letter, 1 lowercase letter and 1 number
        resetPasswordRequest.setPassword("invalid password");
        resetPasswordRequest.setConfirmPassword("invalid password");

        // This user is present in the database
        LocalUser userA = localUserRepository.findByUsernameIgnoreCase("usernameA").get();

        // Create an unexpired token
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setExpireAt(LocalDateTime.now().plusSeconds(900));
        passwordResetToken.setToken("ValidUnexpiredToken");
        passwordResetToken.setLocalUser(userA);
        passwordResetTokenRepository.save(passwordResetToken);

        // Perform
        mockMvc.perform(
                MockMvcRequestBuilders.post("/auth/reset-password")
                        .param("token" , passwordResetToken.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPasswordRequest))
        ).andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException()));
    }

    @Test
    void resetPassword_PasswordsDoNotMatch_returns400() throws Exception {
        ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest();
        // Password must be between 8-16 characters, has at least 1 uppercase letter, 1 lowercase letter and 1 number
        resetPasswordRequest.setPassword("ValidPassword123");
        resetPasswordRequest.setConfirmPassword("ValidPassword12");

        // This user is present in the database
        LocalUser userA = localUserRepository.findByUsernameIgnoreCase("usernameA").get();

        // Create an unexpired token
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setExpireAt(LocalDateTime.now().plusSeconds(900));
        passwordResetToken.setToken("ValidUnexpiredToken");
        passwordResetToken.setLocalUser(userA);
        passwordResetTokenRepository.save(passwordResetToken);

        // Perform
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/auth/reset-password")
                                .param("token" , passwordResetToken.getToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(resetPasswordRequest))
                ).andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(PasswordsDoNotMatchException.class, result.getResolvedException()));
    }

    @Test
    void resetPassword_invalidToken_returns401() throws Exception {
        ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest();
        // Password must be between 8-16 characters, has at least 1 uppercase letter, 1 lowercase letter and 1 number
        resetPasswordRequest.setPassword("ValidPassword123");
        resetPasswordRequest.setConfirmPassword("ValidPassword123");

        String token = "Invalid token";

        // Perform
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/auth/reset-password")
                                .param("token" , token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(resetPasswordRequest))
                ).andExpect(status().isUnauthorized())
                .andExpect(result -> assertInstanceOf(InvalidTokenException.class, result.getResolvedException()));
    }

    @Test
    void resetPassword_expiredToken_returns401() throws Exception {
        ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest();
        // Password must be between 8-16 characters, has at least 1 uppercase letter, 1 lowercase letter and 1 number
        resetPasswordRequest.setPassword("ValidPassword123");
        resetPasswordRequest.setConfirmPassword("ValidPassword123");

        // This user is present in the database
        LocalUser userA = localUserRepository.findByUsernameIgnoreCase("usernameA").get();

        // Create an unexpired token
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setExpireAt(LocalDateTime.now().minusSeconds(1));
        passwordResetToken.setToken("ExpiredToken");
        passwordResetToken.setLocalUser(userA);
        passwordResetTokenRepository.save(passwordResetToken);

        // Perform
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/auth/reset-password")
                                .param("token" , passwordResetToken.getToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(resetPasswordRequest))
                ).andExpect(status().isUnauthorized())
                .andExpect(result -> assertInstanceOf(TokenExpiredException.class, result.getResolvedException()));
    }

    @Test
    void resetPassword_validTokenAndMatchingPassword_returns200AndUpdatePassword() throws Exception {
        ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest();
        // Password must be between 8-16 characters, has at least 1 uppercase letter, 1 lowercase letter and 1 number
        resetPasswordRequest.setPassword("ValidPassword123");
        resetPasswordRequest.setConfirmPassword("ValidPassword123");

        // This user is present in the database
        LocalUser userA = localUserRepository.findByUsernameIgnoreCase("usernameA").get();

        // Create an unexpired token
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setExpireAt(LocalDateTime.now().plusSeconds(900));
        passwordResetToken.setToken("ValidUnexpiredToken");
        passwordResetToken.setLocalUser(userA);
        passwordResetTokenRepository.save(passwordResetToken);

        // Perform
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/auth/reset-password")
                                .param("token" , passwordResetToken.getToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(resetPasswordRequest))
                ).andExpect(status().isOk());

        userA = localUserRepository.findByUsernameIgnoreCase("usernameA").get();

        // Assert
        assertTrue(encryptionService.verifyPassword("ValidPassword123", userA.getPassword()));
    }
}
