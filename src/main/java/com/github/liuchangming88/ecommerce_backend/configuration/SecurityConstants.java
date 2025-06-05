package com.github.liuchangming88.ecommerce_backend.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Arrays;

@Configuration
public class SecurityConstants {

    // List of public endpoints
    public static final String[] PUBLIC_ENDPOINTS = {
            "/auth/login",
            "/auth/register",
            "/auth/verify",
            "/auth/forgot-password",
            "/auth/reset-password",
            "/products",
            "/products/*",
            "/error"
    };

    // Convert to RequestMatchers for use elsewhere
    public static RequestMatcher[] getPublicRequestMatchers() {
        return Arrays.stream(PUBLIC_ENDPOINTS)
                .map(AntPathRequestMatcher::new)
                .toArray(RequestMatcher[]::new);
    }
}