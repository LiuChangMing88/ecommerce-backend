package com.github.liuchangming88.ecommerce_backend.security.oauth2;

import com.github.liuchangming88.ecommerce_backend.model.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.repository.LocalUserRepository;
import com.github.liuchangming88.ecommerce_backend.service.JwtService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final LocalUserRepository localUserRepository;

    public OAuth2AuthenticationSuccessHandler(JwtService jwtService, LocalUserRepository localUserRepository) {
        this.jwtService = jwtService;
        this.localUserRepository = localUserRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {
        // 1. `auth` is the OAuth2AuthenticationToken
        // 2. `auth.getPrincipal()` returns the OAuth2User instance you created
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        // 3. You extract the email (or username) from oauthUser.getAttribute(...)
        String email = oauthUser.getAttribute("email");

        // 4. You re-load (or already have) your LocalUser entity:
        LocalUser user = localUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new IllegalStateException("Could not locate LocalUser for OAuth2 email: " + email)
                );

        // 5. Generate your JWT from that entity
        String token = jwtService.generateJwt(user);

        // 6. Write the token JSON back in the response
        response.setContentType(APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"token\":\"" + token + "\",\"type\":\"Bearer\"}");
    }
}
