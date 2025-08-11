package com.github.liuchangming88.ecommerce_backend.security.oauth2.UserHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

class CustomOAuth2UserServiceTest {

    @Test
    void delegates_and_processes_attributes() {
        // Mocks
        @SuppressWarnings("unchecked")
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = mock(OAuth2UserService.class);
        ProcessOAuth2User processor = mock(ProcessOAuth2User.class);

        // Class under test
        CustomOAuth2UserService sut = new CustomOAuth2UserService(delegate, processor);

        OAuth2UserRequest req = mock(OAuth2UserRequest.class);

        // Stub the delegate to return a raw OAuth2User
        var raw = new DefaultOAuth2User(
                java.util.Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("email", "u@example.com", "name", "U", "family_name", "X"),
                "email");
        when(delegate.loadUser(req)).thenReturn(raw);

        // Stub the processor to return the processed user built from the raw attributes
        var processed = new DefaultOAuth2User(
                java.util.Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("id", 1L, "username", "u", "email", "u@example.com"),
                "email");
        when(processor.processOAuth2User(raw.getAttributes())).thenReturn(processed);

        // Act
        OAuth2User result = sut.loadUser(req);

        // Assert: delegate and processor invoked with expected args,
        // and the delegator returns exactly the processor’s output (same instance)
        verify(delegate).loadUser(req);
        verify(processor).processOAuth2User(raw.getAttributes());
        assertThat(result).isSameAs(processed);
    }
}