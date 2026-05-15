package com.github.rrin.vulyk.service.auth.api;

import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import com.github.rrin.vulyk.dto.auth.LoginRequest;

public interface LoginAuthenticationService {

    UserEntity authenticate(LoginRequest request);
}
