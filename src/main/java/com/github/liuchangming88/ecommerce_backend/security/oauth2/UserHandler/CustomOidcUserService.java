package com.github.liuchangming88.ecommerce_backend.security.oauth2.UserHandler;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOidcUserService
        implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final ProcessOAuth2User processOAuth2User;

    public CustomOidcUserService(ProcessOAuth2User processOAuth2User) {
        this.processOAuth2User = processOAuth2User;
    }

    // Google / OIDC
    @Override
    public OidcUser loadUser(OidcUserRequest req) {
        DefaultOidcUser oidc = (DefaultOidcUser) new OidcUserService().loadUser(req);
        DefaultOAuth2User mapped = processOAuth2User.processOAuth2User(oidc.getAttributes());
        return new DefaultOidcUser(
                mapped.getAuthorities(),
                oidc.getIdToken(),
                oidc.getUserInfo(),
                "email"
        );
    }
}
