package com.github.liuchangming88.ecommerce_backend.security.oauth2.UserHandler;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService delegate;
    private final ProcessOidcUser processor;

    public CustomOidcUserService(OidcUserService delegate, ProcessOidcUser processor) {
        this.delegate = delegate;
        this.processor = processor;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        // 1) Let the default OIDC service fetch and validate the ID token and userinfo.
        OidcUser raw = delegate.loadUser(userRequest);

        // 2) Apply your business logic on attributes (email + email_verified checks, persistence).
        OAuth2User processed = processor.processOidcUser(raw.getAttributes());

        // 3) Merge attributes so downstream code sees id/username/email AND OIDC claims.
        Map<String, Object> merged = new HashMap<>(raw.getAttributes());
        merged.putAll(processed.getAttributes());

        // Build a userInfo from merged claims to carry custom attributes.
        OidcUserInfo userInfo = new OidcUserInfo(merged);

        // 4) Return an OIDC user preserving the original idToken, with your authorities and merged claims.
        // Use "email" as the name attribute to align with your processors.
        return new DefaultOidcUser(
                (java.util.Collection<? extends GrantedAuthority>) processed.getAuthorities(),
                raw.getIdToken(),
                userInfo,
                "email"
        );
    }
}