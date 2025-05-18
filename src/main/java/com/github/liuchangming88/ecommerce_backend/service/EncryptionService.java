package com.github.liuchangming88.ecommerce_backend.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

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
}