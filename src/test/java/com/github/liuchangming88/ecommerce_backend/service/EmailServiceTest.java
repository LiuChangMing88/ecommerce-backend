package com.github.liuchangming88.ecommerce_backend.service;

import com.github.liuchangming88.ecommerce_backend.model.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.PasswordResetToken;
import com.github.liuchangming88.ecommerce_backend.model.VerificationToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    public void sendVerificationEmail_correctlySendsEmail() {
        // Arrange
        VerificationToken token = new VerificationToken();
        LocalUser user = new LocalUser();
        user.setEmail("user@example.com");
        token.setLocalUser(user);
        token.setToken("sample-token");

        // Act
        emailService.sendVerificationEmail(token);

        // Assert
        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    public void sendPasswordResetEmail_correctlySendsEmail() {
        // Arrange
        PasswordResetToken token = new PasswordResetToken();
        LocalUser user = new LocalUser();
        user.setEmail("user@example.com");
        token.setLocalUser(user);
        token.setToken("sample-token");

        // Act
        emailService.sendPasswordResetEmail(token);

        // Assert
        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
