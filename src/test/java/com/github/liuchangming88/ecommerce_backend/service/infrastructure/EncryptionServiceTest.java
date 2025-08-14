package com.github.liuchangming88.ecommerce_backend.service.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    public void setUp() {
        encryptionService = new EncryptionService();
        // Set the saltRounds field to a known value
        ReflectionTestUtils.setField(encryptionService, "saltRounds", 10);
        // Manually call postConstruct to initialize the salt
        encryptionService.postConstruct();
    }

    @Test
    public void encryptPasswordVerifyPassword_correctPassword_returnsTrue() {
        String rawPassword = "mySecretPassword";
        String hashedPassword = encryptionService.encryptPassword(rawPassword);

        // assert
        assertNotNull(hashedPassword);
        assertTrue(encryptionService.verifyPassword(rawPassword, hashedPassword));
    }

    @Test
    public void encryptPasswordVerifyPassword_wrongPassword_returnsFalse() {
        String rawPassword = "mySecretPassword";
        String wrongPassword = "wrongPassword";
        String hashedPassword = encryptionService.encryptPassword(rawPassword);

        // assert
        assertFalse(encryptionService.verifyPassword(wrongPassword, hashedPassword));
    }
}
