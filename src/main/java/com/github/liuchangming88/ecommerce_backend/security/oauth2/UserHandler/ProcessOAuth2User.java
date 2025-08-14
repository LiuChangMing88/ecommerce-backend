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
public class ProcessOAuth2User {
    private final LocalUserRepository localUserRepository;
    private final EncryptionService encryptionService;

    public ProcessOAuth2User(LocalUserRepository localUserRepository,
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

    public DefaultOAuth2User processOAuth2User(Map<String, Object> attrs) {
        String email = (String) attrs.get("email");
        if (email == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info"),
                    "Email not found from OAuth2 provider. This usually means your email is set to private on your Github/Facebook account. Please update your profile settings to make your email address public and try again."
                    // DO NOT prompt the user to enter their email if email is not found, as that might lead to account hijacking
            );
        }

        LocalUser user = localUserRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> {
                    LocalUser u = new LocalUser();
                    u.setEmail(email);
                    u.setUsername(generateUniqueUsername(email));
                    u.setFirstName((String) attrs.getOrDefault("name", ""));
                    u.setLastName((String) attrs.getOrDefault("family_name", ""));
                    // This creates a random password so OAuth2 accounts can only be logged in using OAuth2 services.
                    // Because of this, be sure to prevent local login of oauth2 accounts, as if not, account hijacking might happen
                    u.setPassword(encryptionService.randomPassword());
                    // Always unverified for OAuth2!
                    // This is because the email associated with the account might be unverified (for example the email used to register the facebook account might not have been verified)
                    // This is only a workaround, as, although it has fixed a security vulnerability, it created unpleasant user experience
                    // This is because users will expect everything to be done once they login using OAuth2, but now they have to verify via email.
                    u.setIsEmailVerified(false);
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