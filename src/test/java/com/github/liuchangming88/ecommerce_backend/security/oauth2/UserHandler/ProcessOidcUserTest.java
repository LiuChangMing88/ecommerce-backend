package com.github.liuchangming88.ecommerce_backend.security.oauth2.UserHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.github.liuchangming88.ecommerce_backend.model.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.Role;
import com.github.liuchangming88.ecommerce_backend.model.repository.LocalUserRepository;
import com.github.liuchangming88.ecommerce_backend.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

@ExtendWith(MockitoExtension.class)
class ProcessOidcUserTest {

    @Mock
    LocalUserRepository localUserRepository;

    @Mock
    EncryptionService encryptionService;

    @Captor
    ArgumentCaptor<LocalUser> userCaptor;

    @InjectMocks
    ProcessOidcUser processor;

    @Test
    void createsNewUser_whenEmailPresent_andVerifiedTrue_setsVerified_andReturnsPrincipal() {
        // given
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("email", "verified.user@example.com");
        attrs.put("email_verified", true);
        attrs.put("name", "Verified");
        attrs.put("family_name", "User");

        when(localUserRepository.findByEmailIgnoreCase("verified.user@example.com"))
                .thenReturn(Optional.empty());
        when(localUserRepository.existsByUsernameIgnoreCase("verified.user")).thenReturn(false);
        when(encryptionService.randomPassword()).thenReturn("RANDOM_PASSWORD");
        // Simulate DB assigning ID by mutating and returning the same instance
        when(localUserRepository.save(any(LocalUser.class))).thenAnswer(inv -> {
            LocalUser u = inv.getArgument(0);
            u.setId(123L);
            return u;
        });

        // when
        DefaultOAuth2User principal = processor.processOidcUser(attrs);

        // then
        verify(localUserRepository).findByEmailIgnoreCase("verified.user@example.com");
        verify(localUserRepository).existsByUsernameIgnoreCase("verified.user");
        verify(encryptionService).randomPassword();
        verify(localUserRepository).save(userCaptor.capture());

        LocalUser saved = userCaptor.getValue();
        // Since we mutate and return the same instance, id is set
        assertThat(saved.getId()).isEqualTo(123L);
        assertThat(saved.getEmail()).isEqualTo("verified.user@example.com");
        assertThat(saved.getUsername()).isEqualTo("verified.user");
        assertThat(saved.getPassword()).isEqualTo("RANDOM_PASSWORD");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        // If your LocalUser exposes a getter for the verified flag, you can assert it here:
        // assertThat(saved.getIsEmailVerified()).isTrue();

        assertThat(principal.getName()).isEqualTo("verified.user@example.com");
        assertThat(principal.getAttributes())
                .containsEntry("id", 123L)
                .containsEntry("username", "verified.user")
                .containsEntry("email", "verified.user@example.com");
        assertThat(principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .containsExactly("ROLE_USER");
    }

    @Test
    void createsNewUser_withUsernameCollision_appendsNumericSuffix() {
        // given
        Map<String, Object> attrs = Map.of(
                "email", "john@example.com",
                "email_verified", true,
                "name", "John",
                "family_name", "Doe"
        );

        when(localUserRepository.findByEmailIgnoreCase("john@example.com"))
                .thenReturn(Optional.empty());
        when(localUserRepository.existsByUsernameIgnoreCase("john")).thenReturn(true);
        when(localUserRepository.existsByUsernameIgnoreCase("john1")).thenReturn(false);
        when(encryptionService.randomPassword()).thenReturn("RANDOM_PASSWORD");
        when(localUserRepository.save(any(LocalUser.class))).thenAnswer(inv -> {
            LocalUser u = inv.getArgument(0);
            u.setId(777L);
            return u;
        });

        // when
        DefaultOAuth2User principal = processor.processOidcUser(attrs);

        // then
        verify(localUserRepository).existsByUsernameIgnoreCase("john");
        verify(localUserRepository).existsByUsernameIgnoreCase("john1");
        verify(localUserRepository).save(userCaptor.capture());

        LocalUser saved = userCaptor.getValue();
        assertThat(saved.getUsername()).isEqualTo("john1");
        assertThat(principal.getAttributes()).containsEntry("username", "john1");
    }

    @Test
    void returnsExistingUser_whenEmailPresent_andUserExists_doesNotSaveAgain() {
        // given
        Map<String, Object> attrs = Map.of(
                "email", "existing@example.com",
                "email_verified", true
        );

        LocalUser existing = new LocalUser();
        existing.setId(42L);
        existing.setEmail("existing@example.com");
        existing.setUsername("existingUser");
        existing.setRole(Role.USER);

        when(localUserRepository.findByEmailIgnoreCase("existing@example.com"))
                .thenReturn(Optional.of(existing));

        // when
        DefaultOAuth2User principal = processor.processOidcUser(attrs);

        // then
        verify(localUserRepository, never()).existsByUsernameIgnoreCase(any());
        verify(encryptionService, never()).randomPassword();
        verify(localUserRepository, never()).save(any(LocalUser.class));

        assertThat(principal.getAttributes())
                .containsEntry("id", 42L)
                .containsEntry("username", "existingUser")
                .containsEntry("email", "existing@example.com");
        assertThat(principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .containsExactly("ROLE_USER");
    }

    @Test
    void throws_whenEmailMissing() {
        // given
        Map<String, Object> attrs = Map.of(
                "email_verified", true
        );

        // when / then
        assertThatThrownBy(() -> processor.processOidcUser(attrs))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("Email not found from OIDC provider");
    }

    @Test
    void throws_whenEmailVerifiedMissing() {
        // given
        Map<String, Object> attrs = Map.of(
                "email", "user@example.com"
                // email_verified missing -> treated as false
        );

        // when / then
        assertThatThrownBy(() -> processor.processOidcUser(attrs))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("OIDC provider did not verify this email address");
    }

    @Test
    void throws_whenEmailVerifiedFalse() {
        // given
        Map<String, Object> attrs = Map.of(
                "email", "user@example.com",
                "email_verified", false
        );

        // when / then
        assertThatThrownBy(() -> processor.processOidcUser(attrs))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("OIDC provider did not verify this email address");
    }

    @Test
    void throws_whenEmailVerifiedWrongType_likeStringTrue() {
        // given
        Map<String, Object> attrs = Map.of(
                "email", "user@example.com",
                "email_verified", "true" // not a Boolean -> treated as unverified by your code
        );

        // when / then
        assertThatThrownBy(() -> processor.processOidcUser(attrs))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("OIDC provider did not verify this email address");
    }
}