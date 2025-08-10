package com.github.liuchangming88.ecommerce_backend.security.oauth2.UserHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
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
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

@ExtendWith(MockitoExtension.class)
class ProcessOAuth2UserTest {

    @Mock
    LocalUserRepository localUserRepository;

    @Mock
    EncryptionService encryptionService;

    @Captor
    ArgumentCaptor<LocalUser> userCaptor;

    @InjectMocks
    ProcessOAuth2User processor;

    @Test
    void createsNewUser_whenEmailPresent_andNotExisting_setsUnverified_andReturnsPrincipal() {
        // given
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("email", "new.user@example.com");
        attrs.put("name", "New");
        attrs.put("family_name", "User");

        when(localUserRepository.findByEmailIgnoreCase("new.user@example.com"))
                .thenReturn(Optional.empty());
        // Username base = "new.user" (before @). Ensure it's available
        when(localUserRepository.existsByUsernameIgnoreCase("new.user")).thenReturn(false);
        when(encryptionService.randomPassword()).thenReturn("RANDOM_PASSWORD");
        when(localUserRepository.save(any(LocalUser.class))).thenAnswer(inv -> {
            LocalUser u = inv.getArgument(0);
            u.setId(123L); // simulate DB id
            return u;
        });

        // when
        DefaultOAuth2User principal = processor.processOAuth2User(attrs);

        // then: user persisted with expected fields
        verify(localUserRepository).findByEmailIgnoreCase("new.user@example.com");
        verify(localUserRepository).existsByUsernameIgnoreCase("new.user");
        verify(encryptionService).randomPassword();
        verify(localUserRepository).save(userCaptor.capture());

        LocalUser saved = userCaptor.getValue();
        assertThat(saved.getId()).isEqualTo(123L); // before save, id should be null
        assertThat(saved.getEmail()).isEqualTo("new.user@example.com");
        assertThat(saved.getUsername()).isEqualTo("new.user");
        assertThat(saved.getPassword()).isEqualTo("RANDOM_PASSWORD");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.getIsEmailVerified()).isFalse();

        // and the returned principal mirrors persisted user (id is set by save)
        assertThat(principal.getName()).isEqualTo("new.user@example.com"); // key attribute is "email"
        assertThat(principal.getAttributes())
                .containsEntry("id", 123L)
                .containsEntry("username", "new.user")
                .containsEntry("email", "new.user@example.com");

        assertThat(principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .containsExactly("ROLE_USER");
    }

    @Test
    void createsNewUser_withUsernameCollision_appendsNumericSuffix() {
        // given
        Map<String, Object> attrs = Map.of(
                "email", "john@example.com",
                "name", "John",
                "family_name", "Doe"
        );

        when(localUserRepository.findByEmailIgnoreCase("john@example.com"))
                .thenReturn(Optional.empty());
        // Simulate collision on "john", then "john1" available
        when(localUserRepository.existsByUsernameIgnoreCase("john")).thenReturn(true);
        when(localUserRepository.existsByUsernameIgnoreCase("john1")).thenReturn(false);

        when(encryptionService.randomPassword()).thenReturn("RANDOM_PASSWORD");
        when(localUserRepository.save(any(LocalUser.class))).thenAnswer(inv -> {
            LocalUser u = inv.getArgument(0);
            u.setId(777L);
            return u;
        });

        // when
        DefaultOAuth2User principal = processor.processOAuth2User(attrs);

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
        Map<String, Object> attrs = Map.of("email", "existing@example.com");

        LocalUser existing = new LocalUser();
        existing.setId(42L);
        existing.setEmail("existing@example.com");
        existing.setUsername("existingUser");
        existing.setRole(Role.USER);

        when(localUserRepository.findByEmailIgnoreCase("existing@example.com"))
                .thenReturn(Optional.of(existing));

        // when
        DefaultOAuth2User principal = processor.processOAuth2User(attrs);

        // then: no new save, no username lookup, no random password
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
    void throws_whenEmailMissing_inAttributes() {
        // given
        Map<String, Object> attrs = Map.of("name", "NoEmail User");

        // when / then
        assertThatThrownBy(() -> processor.processOAuth2User(attrs))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("Email not found from OAuth2 provider");
    }
}