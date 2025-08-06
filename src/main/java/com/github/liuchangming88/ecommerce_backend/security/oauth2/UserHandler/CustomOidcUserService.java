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

    private final ProcessOidcUser processOidcUser;

    public CustomOidcUserService(ProcessOidcUser processOidcUser) {
        this.processOidcUser = processOidcUser;
    }

    // Google / OIDC
    @Override
    public OidcUser loadUser(OidcUserRequest req) {
        DefaultOidcUser oidc = (DefaultOidcUser) new OidcUserService().loadUser(req);
        DefaultOAuth2User mapped = processOidcUser.processOidcUser(oidc.getAttributes());
        return new DefaultOidcUser(
                mapped.getAuthorities(),
                oidc.getIdToken(),
                oidc.getUserInfo(),
                "email"
        );
    }
}
