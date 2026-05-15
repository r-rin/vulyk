package com.github.rrin.vulyk.service.profile.impl;

import com.github.rrin.vulyk.dto.user.UserProfileResponse;
import com.github.rrin.vulyk.service.profile.api.ProfileEditFormResolver;
import org.springframework.stereotype.Service;

@Service
public class SecureProfileEditFormResolver implements ProfileEditFormResolver {

    @Override
    public String resolveAction(UserProfileResponse profile) {
        return "/web/profile/edit";
    }
}
