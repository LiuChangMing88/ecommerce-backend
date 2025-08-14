package com.github.liuchangming88.ecommerce_backend.service.user;

import com.github.liuchangming88.ecommerce_backend.model.user.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.user.PasswordResetToken;
import com.github.liuchangming88.ecommerce_backend.model.user.VerificationToken;
import com.github.liuchangming88.ecommerce_backend.model.user.repository.VerificationTokenRepository;
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

    /**
     * Generates a secure random token with 256 bits using SecureRandom
     */

    private static String generateToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * This is for email verification
     */

    public VerificationToken createVerificationToken (LocalUser localUser) {
        String token = generateToken();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setExpireAt(LocalDateTime.now().plusSeconds(emailVerificationExpiryInSeconds));
        verificationToken.setLocalUser(localUser);
        return verificationToken;
    }

    /**
     * This is for password resetting
     */

    public PasswordResetToken createPasswordResetToken (LocalUser localUser) {
        String token = generateToken();
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setToken(token);
        passwordResetToken.setExpireAt(LocalDateTime.now().plusSeconds(passwordResetExpiryInSeconds));
        passwordResetToken.setLocalUser(localUser);
        return passwordResetToken;
    }
}