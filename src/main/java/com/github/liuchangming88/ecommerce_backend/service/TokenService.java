package com.github.liuchangming88.ecommerce_backend.service;

import com.github.liuchangming88.ecommerce_backend.model.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.PasswordResetToken;
import com.github.liuchangming88.ecommerce_backend.model.VerificationToken;
import com.github.liuchangming88.ecommerce_backend.model.repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class TokenService {
    VerificationTokenRepository verificationTokenRepository;

    @Value("${email.verification.expiry.in.seconds}")
    private int emailVerificationExpiryInSeconds;

    @Value("${password.reset.expiry.in.seconds}")
    private int passwordResetExpiryInSeconds;

    public TokenService(VerificationTokenRepository verificationTokenRepository) {
        this.verificationTokenRepository = verificationTokenRepository;
    }

    private static String generateToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    public VerificationToken createVerificationToken (LocalUser localUser) {
        String token = generateToken();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setExpireAt(LocalDateTime.now().plusSeconds(emailVerificationExpiryInSeconds));
        verificationToken.setLocalUser(localUser);
        return verificationToken;
    }

    public PasswordResetToken createPasswordResetToken (LocalUser localUser) {
        String token = generateToken();
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setToken(token);
        passwordResetToken.setExpireAt(LocalDateTime.now().plusSeconds(passwordResetExpiryInSeconds));
        passwordResetToken.setLocalUser(localUser);
        return passwordResetToken;
    }
}