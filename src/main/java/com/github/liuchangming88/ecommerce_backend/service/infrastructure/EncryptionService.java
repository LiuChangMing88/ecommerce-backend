package com.github.liuchangming88.ecommerce_backend.service.infrastructure;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EncryptionService {

    @Value("${encryption.salt.rounds}")
    private int saltRounds;

    private String salt;

    // Post construct because of how spring's IOC injects data
    @PostConstruct
    public void postConstruct() {
        this.salt = BCrypt.gensalt(saltRounds);
    }

    // Encrypt password
    public String encryptPassword(String password) {
        return BCrypt.hashpw(password, salt);
    }

    // Verify password
    public boolean verifyPassword(String password, String hashedPw) {
        return BCrypt.checkpw(password, hashedPw);
    }

    // Generate random password to satisfy non-null constraint for OAuth2 authenticated users (the password will never be used)
    public String randomPassword() {
        // 1) Generate a random base password (32 chars, no dashes)
        String raw = UUID.randomUUID().toString().replace("-", "");
        // 2) Encrypt/hash it using your EncryptionService
        return encryptPassword(raw);
    }
}