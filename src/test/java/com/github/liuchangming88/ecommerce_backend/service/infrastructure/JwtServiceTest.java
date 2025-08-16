package com.github.liuchangming88.ecommerce_backend.service.infrastructure;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.github.liuchangming88.ecommerce_backend.model.user.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

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
    public void generateJwtParseJwt_correctSignature_returnsCorrectParse() {
        // Arrange
        LocalUser user = new LocalUser();
        user.setUsername("testUser");
        user.setRole(Role.USER);

        // Act
        String token = jwtService.generateJwt(user);
        String extractedUsername = jwtService.getSubject(token);

        // Assert
        assertNotNull(token, "Token should not be null");
        assertEquals("testUser", extractedUsername);
    }

    @Test
    public void generateJwtParseJwt_wrongSignature_returnsException() {
        // Arrange
        LocalUser user = new LocalUser();
        user.setUsername("testUser");

        // Act
        String token = JWT.create()
                .withSubject( user.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600))
                .withIssuer("test-issuer")
                .sign(Algorithm.HMAC256("gibberish-secret-key"));

        // Assert
        assertThrows(
                JWTVerificationException.class,
                () -> jwtService.getSubject(token)
        );
    }

    @Test
    public void generateJwtParseJwt_correctSignatureWrongIssuer_returnsException() {
        // Arrange
        LocalUser user = new LocalUser();
        user.setUsername("testUser");

        // Act
        String token = JWT.create()
                .withSubject( user.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600))
                .withIssuer("gibberish-issuer")
                .sign(Algorithm.HMAC256("test-secret-key"));

        // Assert
        assertThrows(
                JWTVerificationException.class,
                () -> jwtService.getSubject(token)
        );
    }

    @Test
    public void generateJwtParseJwt_expired_returnsException() {
        // Arrange
        LocalUser user = new LocalUser();
        user.setUsername("testUser");

        // Act
        String token = JWT.create()
                .withSubject( user.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis() - 1000))
                .withIssuer("test-issuer")
                .sign(Algorithm.HMAC256("test-secret-key"));

        // Assert
        assertThrows(
                TokenExpiredException.class,
                () -> jwtService.getSubject(token)
        );
    }
}
