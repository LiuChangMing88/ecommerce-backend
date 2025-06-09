package com.github.liuchangming88.ecommerce_backend.security.oauth2.UserHandler;

import com.github.liuchangming88.ecommerce_backend.model.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.Role;
import com.github.liuchangming88.ecommerce_backend.model.repository.LocalUserRepository;
import com.github.liuchangming88.ecommerce_backend.service.EncryptionService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ProcessOAuth2User {
    private final LocalUserRepository localUserRepository;
    private final EncryptionService encryptionService;

    public ProcessOAuth2User(LocalUserRepository localUserRepository,
                                 EncryptionService encryptionService) {
        this.localUserRepository = localUserRepository;
        this.encryptionService = encryptionService;
    }
    /**
     * Derive a unique username from email (e.g. local part + random suffix
     * if already taken).
     */
    private String generateUniqueUsername(String email) {
        String base = email.split("@")[0];
        String username = base;
        int suffix = 0;
        while (localUserRepository.existsByUsernameIgnoreCase(username)) {
            username = base + (++suffix);
        }
        return username;
    }

    public DefaultOAuth2User processOAuth2User(Map<String, Object> attrs) {
        // 1. Extract email
        String email = (String) attrs.get("email");
        if (email == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info"),
                    "Email not found from OAuth2 provider"
            );
        }

        // 2. Find or create LocalUser
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

        // 3. Build authorities
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        // 4. Build principal attributes to expose
        Map<String, Object> principalAttrs = Map.of(
                "id",       user.getId(),
                "username", user.getUsername(),
                "email",    user.getEmail()
        );

        // 5. Return DefaultOAuth2User
        return new DefaultOAuth2User(
                authorities,
                principalAttrs,
                "email"  // this key refers to getName()
        );
    }
}
