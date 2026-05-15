package com.github.rrin.vulyk.service.auth.impl;

import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import com.github.rrin.vulyk.dto.auth.LoginRequest;
import com.github.rrin.vulyk.exception.InvalidCredentials;
import com.github.rrin.vulyk.repository.UserRepository;
import com.github.rrin.vulyk.service.auth.api.LoginAuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecureLoginAuthenticationService implements LoginAuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    @Override
    public UserEntity authenticate(LoginRequest request) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            request.getIdentifier(),
            request.getPassword()
        );
        authenticationManager.authenticate(authentication);

        return userRepository.findByEmailOrUsername(request.getIdentifier())
            .orElseThrow(() -> new InvalidCredentials("Invalid email or password"));
    }
}
