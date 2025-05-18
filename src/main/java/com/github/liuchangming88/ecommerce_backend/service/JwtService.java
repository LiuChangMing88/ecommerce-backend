package com.github.liuchangming88.ecommerce_backend.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.github.liuchangming88.ecommerce_backend.model.LocalUser;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.algorithm.key}")
    private String algorithmKey;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiry.in.seconds}")
    private int expiryInSeconds;

    private Algorithm algorithm;

    private static final String USERNAME_KEY = "USERNAME";

    // Post construct because of how spring's IOC injects data
    @PostConstruct
    public void postConstruct() {
        this.algorithm = Algorithm.HMAC256(algorithmKey);
    }

    // Generate jwt
    public String generateJwt(LocalUser localUser) {
        return JWT.create()
                .withClaim(USERNAME_KEY, localUser.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis() + (expiryInSeconds * 1000L)))
                .withIssuer(issuer)
                .sign(algorithm);
    }

    // Decode jwt to get username
    public String getUsername(String token) {
        return JWT.decode(token).getClaim(USERNAME_KEY).asString();
    }
}
