package com.github.liuchangming88.ecommerce_backend.service;

import com.github.liuchangming88.ecommerce_backend.model.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.VerificationToken;
import com.github.liuchangming88.ecommerce_backend.model.repository.VerificationTokenRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
public class VerificationServiceTest {
    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @InjectMocks
    private TokenService tokenService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(tokenService, "emailExpiryInSeconds", 86400);
    }

    @Test
    public void createVerificationToken_returnsVerificationToken() {
        // Arrange
        LocalUser localUser = new LocalUser();

        // Execute
        VerificationToken verificationToken = tokenService.createVerificationToken(localUser);

        // Assert
        Assertions.assertEquals(localUser, verificationToken.getLocalUser());
        Assertions.assertNotNull(verificationToken.getToken());
        Assertions.assertFalse(verificationToken.getExpireAt().isBefore(LocalDateTime.now()));
    }
}
