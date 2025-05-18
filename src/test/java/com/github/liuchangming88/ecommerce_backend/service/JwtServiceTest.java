package com.github.liuchangming88.ecommerce_backend.service;

import com.github.liuchangming88.ecommerce_backend.model.LocalUser;
import com.github.liuchangming88.ecommerce_backend.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    public void setUp() {
        jwtService = new JwtService();
        // Inject test-specific values
        ReflectionTestUtils.setField(jwtService, "algorithmKey", "test-secret-key");
        ReflectionTestUtils.setField(jwtService, "issuer", "test-issuer");
        ReflectionTestUtils.setField(jwtService, "expiryInSeconds", 3600);
        jwtService.postConstruct();
    }

    @Test
    public void testGenerateAndParseJwt() {
        // Arrange
        LocalUser user = new LocalUser();
        user.setUsername("testuser");

        // Act
        String token = jwtService.generateJwt(user);
        String extractedUsername = jwtService.getUsername(token);

        // Assert
        assertNotNull(token, "Token should not be null");
        assertEquals("testuser", extractedUsername, "Extracted username should match the original");
    }
}
