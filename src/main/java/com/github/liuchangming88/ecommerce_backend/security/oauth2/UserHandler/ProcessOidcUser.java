package com.github.liuchangming88.ecommerce_backend.security.oauth2.UserHandler;

import com.github.liuchangming88.ecommerce_backend.model.user.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.user.Role;
import com.github.liuchangming88.ecommerce_backend.model.user.repository.LocalUserRepository;
import com.github.liuchangming88.ecommerce_backend.service.infrastructure.EncryptionService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ProcessOidcUser {
    private final LocalUserRepository localUserRepository;
    private final EncryptionService encryptionService;

    public ProcessOidcUser(LocalUserRepository localUserRepository,
                            EncryptionService encryptionService) {
        this.localUserRepository = localUserRepository;
        this.encryptionService = encryptionService;
    }

    private String generateUniqueUsername(String email) {
        String base = email.split("@")[0];
        String username = base;
        int suffix = 0;
        while (localUserRepository.existsByUsernameIgnoreCase(username)) {
            username = base + (++suffix);
        }
        return username;
    }

    public DefaultOAuth2User processOidcUser(Map<String, Object> attrs) {
        String email = (String) attrs.get("email");
        Object emailVerifiedObj = attrs.get("email_verified");
        boolean emailVerified = emailVerifiedObj instanceof Boolean && (Boolean) emailVerifiedObj;

        if (email == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info"),
                    "Email not found from OIDC provider"
            );
        }

        if (!emailVerified) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("unverified_email"),
                    "OIDC provider did not verify this email address"
            );
        }

        LocalUser user = localUserRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> {
                    LocalUser u = new LocalUser();
                    u.setEmail(email);
                    u.setUsername(generateUniqueUsername(email));
                    u.setFirstName((String) attrs.getOrDefault("name", ""));
                    u.setLastName((String) attrs.getOrDefault("family_name", ""));
                    u.setPassword(encryptionService.randomPassword());
                    u.setIsEmailVerified(true);
                    u.setRole(Role.USER);
                    return localUserRepository.save(u);
                });

        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        Map<String, Object> principalAttrs = Map.of(
                "id",       user.getId(),
                "username", user.getUsername(),
                "email",    user.getEmail()
        );

        return new DefaultOAuth2User(
                authorities,
                principalAttrs,
                "email"
        );
    }
}