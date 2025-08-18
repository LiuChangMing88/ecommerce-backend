package com.github.liuchangming88.ecommerce_backend.security;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.github.liuchangming88.ecommerce_backend.model.user.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.user.repository.LocalUserRepository;
import com.github.liuchangming88.ecommerce_backend.service.infrastructure.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtRequestFilterTest {

    JwtService jwtService = mock(JwtService.class);
    LocalUserRepository userRepo = mock(LocalUserRepository.class);
    JwtRequestFilter filter = new JwtRequestFilter(jwtService, userRepo);

    @AfterEach
    void clearCtx() {
        SecurityContextHolder.clearContext();
    }

    private LocalUser verifiedUser(String username) {
        LocalUser u = new LocalUser();
        u.setUsername(username);
        u.setIsEmailVerified(true);
        return u;
    }

    @Test
    void missingAuthorizationHeader_returns401_json() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/private");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, resp, chain);

        assertThat(resp.getStatus()).isEqualTo(401);
        assertThat(resp.getContentType()).isEqualTo("application/json");
        String body = resp.getContentAsString();
        assertThat(body).contains("\"error\": \"Unauthorized\"")
                .contains("\"message\": \"Missing or invalid Authorization header\"");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void validToken_setsAuthentication_withDetails() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod("GET");
        req.setRequestURI("/api/protected");
        req.addHeader("Authorization", "Bearer goodToken");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtService.getSubject("goodToken")).thenReturn("john");
        when(jwtService.getRole("goodToken")).thenReturn("USER");
        when(userRepo.findByUsernameIgnoreCase("john")).thenReturn(Optional.of(verifiedUser("john")));

        filter.doFilter(req, resp, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(LocalUser.class);
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        // Kill mutant removing setDetails()
        assertThat(auth.getDetails()).isNotNull();
    }

    @Test
    void userNotFound_returns401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/x");
        req.addHeader("Authorization", "Bearer t1");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        when(jwtService.getSubject("t1")).thenReturn("ghost");
        when(userRepo.findByUsernameIgnoreCase("ghost")).thenReturn(Optional.empty());

        filter.doFilter(req, resp, new MockFilterChain());

        assertThat(resp.getStatus()).isEqualTo(401);
        assertThat(resp.getContentAsString()).contains("Invalid or expired authentication token");
    }

    @Test
    void unverifiedEmail_returns403_withBadRequestErrorField() throws Exception {
        LocalUser unverified = new LocalUser();
        unverified.setUsername("alice");
        unverified.setIsEmailVerified(false);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/y");
        req.addHeader("Authorization", "Bearer t2");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        when(jwtService.getSubject("t2")).thenReturn("alice");
        when(userRepo.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(unverified));

        filter.doFilter(req, resp, new MockFilterChain());

        assertThat(resp.getStatus()).isEqualTo(403);
        String body = resp.getContentAsString();
        assertThat(body).contains("\"error\": \"Bad Request\"")
                .contains("\"message\": \"Email not verified\"");
    }

    @Test
    void jwtDecodeException_returns401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/dec");
        req.addHeader("Authorization", "Bearer bad");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        when(jwtService.getSubject("bad")).thenThrow(new JWTDecodeException("broken"));

        filter.doFilter(req, resp, new MockFilterChain());

        assertThat(resp.getStatus()).isEqualTo(401);
        assertThat(resp.getContentAsString()).contains("Invalid JWT token");
    }

    @Test
    void tokenExpired_returns401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/exp");
        req.addHeader("Authorization", "Bearer exp");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        when(jwtService.getSubject("exp")).thenThrow(new TokenExpiredException("expired", Instant.now()));

        filter.doFilter(req, resp, new MockFilterChain());

        assertThat(resp.getStatus()).isEqualTo(401);
        assertThat(resp.getContentAsString()).contains("Token has expired");
    }

    @Test
    void verificationFailure_returns401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/ver");
        req.addHeader("Authorization", "Bearer v");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        when(jwtService.getSubject("v")).thenThrow(new JWTVerificationException("sig mismatch"));

        filter.doFilter(req, resp, new MockFilterChain());

        assertThat(resp.getStatus()).isEqualTo(401);
        assertThat(resp.getContentAsString()).contains("Token verification failed");
    }

    @Test
    void errorResponses_setContentType_andBodyNotEmpty() throws Exception {
        // Trigger decode error
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/body");
        req.addHeader("Authorization", "Bearer b");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        when(jwtService.getSubject("b")).thenThrow(new JWTDecodeException("x"));
        filter.doFilter(req, resp, new MockFilterChain());

        assertThat(resp.getContentType()).isEqualTo("application/json"); // kills removed setContentType mutant
        assertThat(resp.getContentAsString()).isNotBlank(); // kills removed println mutant
    }
}