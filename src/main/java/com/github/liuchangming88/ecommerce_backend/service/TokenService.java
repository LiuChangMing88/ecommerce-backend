package com.github.liuchangming88.ecommerce_backend.service;

import com.github.liuchangming88.ecommerce_backend.model.LocalUser;
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

    @Value("${email.expiry.in.seconds}")
    private int emailExpiryInSeconds;

    public TokenService(VerificationTokenRepository verificationTokenRepository) {
        this.verificationTokenRepository = verificationTokenRepository;
    }

    private static String generateToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    public VerificationToken createVerificationToken(LocalUser localUser) {
        String token = generateToken();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setExpireAt(LocalDateTime.now().plusSeconds(emailExpiryInSeconds));
        verificationToken.setLocalUser(localUser);
        return verificationToken;
    }
}
