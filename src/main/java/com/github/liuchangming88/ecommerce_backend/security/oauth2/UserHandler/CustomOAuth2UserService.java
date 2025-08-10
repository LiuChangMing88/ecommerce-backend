package com.github.liuchangming88.ecommerce_backend.security.oauth2.UserHandler;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService
        implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;
    private final ProcessOAuth2User processor;

    public CustomOAuth2UserService(
            @Qualifier("defaultOAuth2UserService") OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate,
            ProcessOAuth2User processor) {
        this.delegate = delegate;
        this.processor = processor;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) {
        OAuth2User raw = delegate.loadUser(req);
        return processor.processOAuth2User(raw.getAttributes());
    }
}

