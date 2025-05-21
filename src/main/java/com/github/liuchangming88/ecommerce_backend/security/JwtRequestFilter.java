package com.github.liuchangming88.ecommerce_backend.security;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.github.liuchangming88.ecommerce_backend.model.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.repository.LocalUserRepository;
import com.github.liuchangming88.ecommerce_backend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    JwtService jwtService;
    LocalUserRepository localUserRepository;

    public JwtRequestFilter(JwtService jwtService, LocalUserRepository localUserRepository) {
        this.jwtService = jwtService;
        this.localUserRepository = localUserRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String tokenHeader = request.getHeader("Authorization");
        // Check if the header is present or valid
        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header");
            filterChain.doFilter(request, response);
            return;
        }

        // Now process the token:
        String token = tokenHeader.substring(7);

        try {
            // Get user
            String username = jwtService.getUsername(token);
            Optional<LocalUser> optionalLocalUser = localUserRepository.findByUsernameIgnoreCase(username);
            if (optionalLocalUser.isPresent()) {
                LocalUser localUser = optionalLocalUser.get();
                // If unverified but somehow got jwt token
                if (!localUser.getIsEmailVerified())
                    return;
                // Build the authentication to then pass it into the security context.
                // The authentication holds:
                // 1. The user (the principal)
                // 2. The credentials (which is normally password), but in this case, it can be null because UserService already handled password authentication
                // 3. The list of roles (currently empty)
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(localUser, null, new ArrayList<>());
                // Populate the authentication token even more with: remote IP address and session ID (via WebAuthenticationDetailsSource)
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // Put the authentication into the security context via security context holder
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        catch (JWTDecodeException ex) {
            logger.warn("Invalid Jwt token or token has expired: " + ex.getMessage());
        }
        finally {
            filterChain.doFilter(request, response);
        }
    }
}
