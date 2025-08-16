package com.github.liuchangming88.ecommerce_backend.security;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.github.liuchangming88.ecommerce_backend.configuration.SecurityConstants;
import com.github.liuchangming88.ecommerce_backend.model.user.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.user.repository.LocalUserRepository;
import com.github.liuchangming88.ecommerce_backend.service.infrastructure.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    JwtService jwtService;
    LocalUserRepository localUserRepository;

    public JwtRequestFilter(JwtService jwtService, LocalUserRepository localUserRepository) {
        this.jwtService = jwtService;
        this.localUserRepository = localUserRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return Arrays.stream(SecurityConstants.PUBLIC_ENDPOINTS)
                .anyMatch(pattern -> new AntPathRequestMatcher(pattern).matches(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String tokenHeader = request.getHeader("Authorization");
        // Check if the header is present or valid
        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header");
            sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }

        // Now process the token:
        String token = tokenHeader.substring(7);

        try {
            // Get user
            String username = jwtService.getSubject(token);
            Optional<LocalUser> optionalLocalUser = localUserRepository.findByUsernameIgnoreCase(username);
            if (optionalLocalUser.isEmpty()) {
                logger.warn("No user found for the provided JWT token. The token is either empty or invalid");
                sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired authentication token. The token is either empty or invalid");
                return;
            }
            LocalUser localUser = optionalLocalUser.get();
            // If unverified but somehow got jwt token
            if (!localUser.getIsEmailVerified()) {
                sendErrorResponse(request, response, HttpServletResponse.SC_FORBIDDEN, "Email not verified");
                return;
            }
            // Build the authentication to then pass it into the security context.
            // The authentication holds:
            // 1. The user (the principal)
            // 2. The credentials (which is normally password), but in this case, it can be null because UserService already handled password authentication
            // 3. The role (as a singleton list)
            String role = jwtService.getRole(token);
            List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(localUser, null, authorities);
            // Populate the authentication token even more with: remote IP address and session ID (via WebAuthenticationDetailsSource)
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            // Put the authentication into the security context via security context holder
            SecurityContextHolder.getContext().setAuthentication(authentication);
            // Only proceed if authentication is successful
            filterChain.doFilter(request, response);
        }
        catch (JWTDecodeException ex) {
            logger.warn("Invalid JWT token: " + ex.getMessage());
            sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
        } catch (TokenExpiredException ex) {
            logger.warn("Token has expired: " + ex.getMessage());
            sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Token has expired");
        }
        catch (JWTVerificationException ex) {
            logger.warn("Token verification failed: " + ex.getMessage());
            sendErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Token verification failed");
        }
    }

    // Helper method to send the error response
    private void sendErrorResponse(HttpServletRequest request, HttpServletResponse response, int status, String message) throws IOException {
        response.setContentType("application/json");
        response.setStatus(status);

        // Get current timestamp
        String currentTime = LocalDateTime.now().toString();
        // Get request path
        String path = request.getRequestURI();
        // Determine error type based on status code
        String error = (status == HttpServletResponse.SC_UNAUTHORIZED) ? "Unauthorized" : "Bad Request";

        // Build the JSON response
        String jsonResponse = String.format(
                "{ \"timestamp\": \"%s\", \"status\": %d, \"error\": \"%s\", \"message\": \"%s\", \"path\": \"%s\" }",
                currentTime, status, error, message, path
        );

        response.getOutputStream().println(jsonResponse);
    }
}
