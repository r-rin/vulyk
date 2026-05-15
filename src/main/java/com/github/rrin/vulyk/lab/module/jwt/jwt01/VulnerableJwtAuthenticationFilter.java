package com.github.rrin.vulyk.lab.module.jwt.jwt01;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.rrin.vulyk.domain.entity.user.UserRole;
import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.lab.service.LabProgressService;
import com.github.rrin.vulyk.repository.UserRepository;
import com.github.rrin.vulyk.security.JwtAuthenticationFilter;
import com.github.rrin.vulyk.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Primary
@Component
@ConditionalOnLabEnabled(JwtWeakSecretLab.LAB_ID)
public class VulnerableJwtAuthenticationFilter extends JwtAuthenticationFilter {

    private static final String JWT_COOKIE_NAME = "VULYK_TOKEN";

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final LabProgressService labProgressService;

    public VulnerableJwtAuthenticationFilter(
        JwtTokenProvider tokenProvider,
        UserDetailsService userDetailsService,
        UserRepository userRepository,
        @Lazy LabProgressService labProgressService
    ) {
        super(tokenProvider, userDetailsService);
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.labProgressService = labProgressService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            DecodedJWT jwt = tokenProvider.verifyToken(token);
            String username = jwt.getSubject();
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            String claimedRole = jwt.getClaim("role").asString();
            Authentication auth = new UsernamePasswordAuthenticationToken(
                username,
                null,
                toAuthorities(claimedRole, userDetails)
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            maybeTrackForgedAdminToken(username, claimedRole);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (JWT_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private List<SimpleGrantedAuthority> toAuthorities(String claimedRole, UserDetails userDetails) {
        if (claimedRole == null || claimedRole.isBlank()) {
            return userDetails.getAuthorities().stream()
                .map(authority -> new SimpleGrantedAuthority(authority.getAuthority()))
                .toList();
        }

        String normalizedRole = claimedRole.trim().toUpperCase(Locale.ROOT);
        return List.of(new SimpleGrantedAuthority("ROLE_" + normalizedRole));
    }

    private void maybeTrackForgedAdminToken(String username, String claimedRole) {
        if (!"ADMIN".equalsIgnoreCase(claimedRole)) {
            return;
        }

        userRepository.findByEmail(username)
            .filter(user -> user.getRole() != UserRole.ADMIN)
            .ifPresent(user -> labProgressService.completeStateTask(
                JwtWeakSecretLab.LAB_ID,
                JwtWeakSecretLab.TASK_FORGE_ADMIN_TOKEN,
                "sub=" + username + ":claimedRole=" + claimedRole
            ));
    }
}


