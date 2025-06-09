package com.github.liuchangming88.ecommerce_backend.security.oauth2.UserHandler;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService
        implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final ProcessOAuth2User processOAuth2User;

    public CustomOAuth2UserService(ProcessOAuth2User processOAuth2User) {
        this.processOAuth2User = processOAuth2User;
    }

    // GitHub / other OAuth2
    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(req);
        return processOAuth2User.processOAuth2User(oAuth2User.getAttributes());
    }
}

