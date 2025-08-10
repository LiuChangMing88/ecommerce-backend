package com.github.liuchangming88.ecommerce_backend.security.oauth2.UserHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

class CustomOidcUserServiceTest {

    @Test
    void delegates_to_default_service_then_processes_and_returns_oidc_user_with_merged_claims() {
        OidcUserService delegate = mock(OidcUserService.class);
        ProcessOidcUser processor = mock(ProcessOidcUser.class);
        CustomOidcUserService sut = new CustomOidcUserService(delegate, processor);

        OidcUserRequest req = mock(OidcUserRequest.class);

        // Build a raw OIDC user from the delegate with typical claims
        OidcIdToken idToken = OidcIdToken.withTokenValue("id-token")
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(600))
                .claim("sub", "sub-123")
                .claim("email", "user@example.com")
                .claim("email_verified", true)
                .build();
        OidcUserInfo rawUserInfo = new OidcUserInfo(Map.of("given_name", "User", "family_name", "Example"));
        OidcUser raw = new DefaultOidcUser(Set.of(new SimpleGrantedAuthority("ROLE_USER")), idToken, rawUserInfo, "email");
        when(delegate.loadUser(req)).thenReturn(raw);

        // Processor enriches/validates and outputs your canonical principal attributes
        DefaultOAuth2User processed = new DefaultOAuth2User(
                Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("id", 1L, "username", "user", "email", "user@example.com"),
                "email"
        );
        when(processor.processOidcUser(raw.getAttributes())).thenReturn(processed);

        // Call SUT
        OidcUser result = sut.loadUser(req);

        // Verify delegation and processing
        verify(delegate).loadUser(req);
        verify(processor).processOidcUser(raw.getAttributes());

        // Result is an OIDC user preserving idToken and having merged attributes
        assertThat(result.getIdToken().getTokenValue()).isEqualTo("id-token");
        assertThat(result.getAuthorities())
                .extracting(org.springframework.security.core.GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER");

        // Attributes contain both OIDC claims and your processed fields
        assertThat(result.getAttributes())
                .containsEntry("sub", "sub-123")
                .containsEntry("email", "user@example.com")
                .containsEntry("email_verified", true)
                .containsEntry("id", 1L)
                .containsEntry("username", "user");

        // Name attribute uses email as configured
        assertThat(result.getName()).isEqualTo("user@example.com");
    }
}