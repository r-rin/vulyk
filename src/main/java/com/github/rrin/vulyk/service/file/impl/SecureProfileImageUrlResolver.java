package com.github.rrin.vulyk.service.file.impl;

import com.github.rrin.vulyk.service.file.api.ProfileImageUrlResolver;
import org.springframework.stereotype.Service;

@Service
public class SecureProfileImageUrlResolver implements ProfileImageUrlResolver {

    @Override
    public String resolve(Long profilePictureId) {
        if (profilePictureId == null) {
            return null;
        }
        return "/files/" + profilePictureId;
    }
}
