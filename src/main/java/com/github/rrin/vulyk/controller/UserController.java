package com.github.rrin.vulyk.controller;

import com.github.rrin.vulyk.service.UserService;
import com.github.rrin.vulyk.web.dto.user.UpdateProfileRequest;
import com.github.rrin.vulyk.web.dto.user.UserProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal String principalEmail) {
        return userService.getProfile(principalEmail);
    }

    @PutMapping("/me")
    public UserProfileResponse updateProfile(
        @AuthenticationPrincipal String principalEmail,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        return userService.updateProfile(principalEmail, request);
    }
}
