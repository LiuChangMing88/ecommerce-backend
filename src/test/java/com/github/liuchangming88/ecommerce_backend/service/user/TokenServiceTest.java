package com.github.liuchangming88.ecommerce_backend.service.user;

import com.github.liuchangming88.ecommerce_backend.model.user.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.user.PasswordResetToken;
import com.github.liuchangming88.ecommerce_backend.model.user.VerificationToken;
import com.github.liuchangming88.ecommerce_backend.model.user.repository.VerificationTokenRepository;
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
public class TokenServiceTest {
    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @InjectMocks
    private TokenService tokenService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(tokenService, "emailVerificationExpiryInSeconds", 86400);
        ReflectionTestUtils.setField(tokenService, "passwordResetExpiryInSeconds", 900);
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
        Assertions.assertEquals(43, verificationToken.getToken().length());
        Assertions.assertFalse(verificationToken.getExpireAt().isBefore(LocalDateTime.now()));
    }

    @Test
    public void createPasswordResetToken_returnsPasswordResetToken() {
        // Arrange
        LocalUser localUser = new LocalUser();

        // Execute
        PasswordResetToken passwordResetToken = tokenService.createPasswordResetToken(localUser);

        // Assert
        Assertions.assertEquals(localUser, passwordResetToken.getLocalUser());
        Assertions.assertNotNull(passwordResetToken.getToken());
        Assertions.assertEquals(43, passwordResetToken.getToken().length());
        Assertions.assertFalse(passwordResetToken.getExpireAt().isBefore(LocalDateTime.now()));
    }
}
