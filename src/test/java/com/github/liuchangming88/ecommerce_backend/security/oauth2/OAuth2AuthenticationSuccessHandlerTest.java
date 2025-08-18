package com.github.liuchangming88.ecommerce_backend.security.oauth2;

import com.github.liuchangming88.ecommerce_backend.model.user.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.user.repository.LocalUserRepository;
import com.github.liuchangming88.ecommerce_backend.service.infrastructure.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

class OAuth2AuthenticationSuccessHandlerTest {

    JwtService jwtService = mock(JwtService.class);
    LocalUserRepository userRepo = mock(LocalUserRepository.class);
    OAuth2AuthenticationSuccessHandler handler =
            new OAuth2AuthenticationSuccessHandler(jwtService, userRepo);

    @AfterEach
    void tearDown() {
        // nothing
    }

    @Test
    void success_writesTokenJson() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        OAuth2User oAuth2User = mock(OAuth2User.class);
        when(oAuth2User.getAttribute("email")).thenReturn("User@Example.com");

        LocalUser user = new LocalUser();
        user.setEmail("user@example.com");
        when(userRepo.findByEmailIgnoreCase("User@Example.com"))
                .thenReturn(Optional.of(user));

        when(jwtService.generateJwt(user)).thenReturn("JWT_TOKEN");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(oAuth2User);

        handler.onAuthenticationSuccess(req, resp, auth);

        // repo call with exact email string
        verify(userRepo).findByEmailIgnoreCase("User@Example.com");
        // jwt generation with entity captured
        ArgumentCaptor<LocalUser> userCap = ArgumentCaptor.forClass(LocalUser.class);
        verify(jwtService).generateJwt(userCap.capture());
        assertThat(userCap.getValue()).isSameAs(user);

        assertThat(resp.getContentType()).isEqualTo(APPLICATION_JSON_VALUE);
        String body = resp.getContentAsString();
        assertThat(body).isEqualTo("{\"token\":\"JWT_TOKEN\",\"type\":\"Bearer\"}");
    }

    @Test
    void missingUser_throws() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        OAuth2User oAuth2User = mock(OAuth2User.class);
        when(oAuth2User.getAttribute("email")).thenReturn("missing@example.com");

        when(userRepo.findByEmailIgnoreCase("missing@example.com"))
                .thenReturn(Optional.empty());

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(oAuth2User);

        assertThatThrownBy(() -> handler.onAuthenticationSuccess(req, resp, auth))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not locate LocalUser");

        // No token generation
        verify(jwtService, never()).generateJwt(any());
    }

    @Test
    void nullEmailAttribute_throwsIllegalState() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        OAuth2User oAuth2User = mock(OAuth2User.class);
        when(oAuth2User.getAttribute("email")).thenReturn(null);

        when(userRepo.findByEmailIgnoreCase(null)).thenReturn(Optional.empty());

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(oAuth2User);

        assertThatThrownBy(() -> handler.onAuthenticationSuccess(req, resp, auth))
                .isInstanceOf(IllegalStateException.class);

        verify(jwtService, never()).generateJwt(any());
    }
}