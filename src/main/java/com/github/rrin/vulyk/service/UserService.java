package com.github.rrin.vulyk.service;

import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import com.github.rrin.vulyk.domain.entity.user.UserRole;
import com.github.rrin.vulyk.exception.InvalidCredentials;
import com.github.rrin.vulyk.exception.ValidationException;
import com.github.rrin.vulyk.repository.UserRepository;
import com.github.rrin.vulyk.security.JwtTokenProvider;
import com.github.rrin.vulyk.web.dto.auth.AuthResponse;
import com.github.rrin.vulyk.web.dto.auth.LoginRequest;
import com.github.rrin.vulyk.web.dto.auth.RegisterRequest;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ValidationException("Email is already in use");
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ValidationException("Username is already taken");
        }

        UserEntity user = UserEntity.builder()
            .username(request.getUsername())
            .name(request.getName())
            .bio(request.getBio())
            .email(normalizedEmail)
            .phoneNumber(request.getPhoneNumber())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(UserRole.USER)
            .build();

        userRepository.save(user);

        String token = tokenProvider.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            request.getIdentifier(),
            request.getPassword()
        );
        authenticationManager.authenticate(authentication);

        UserEntity user = userRepository.findByEmailOrUsername(request.getIdentifier())
            .orElseThrow(() -> new InvalidCredentials("Invalid email or password"));

        String token = tokenProvider.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
