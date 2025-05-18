package com.github.liuchangming88.ecommerce_backend.service;

import com.github.liuchangming88.ecommerce_backend.model.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.VerificationToken;
import com.github.liuchangming88.ecommerce_backend.model.repository.VerificationTokenRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VerificationServiceTest {
    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @InjectMocks
    private VerificationTokenService verificationTokenService;

    @Test
    public void createVerificationToken_returnsVerificationToken() {
        // Arrange
        LocalUser localUser = new LocalUser();

        // Execute
        VerificationToken verificationToken = verificationTokenService.createVerificationToken(localUser);

        // Assert
        Assertions.assertEquals(localUser, verificationToken.getLocalUser());
        Assertions.assertNotNull(verificationToken.getToken());
        Assertions.assertFalse(verificationToken.getCreatedAt().isAfter(LocalDateTime.now()));
    }
}
