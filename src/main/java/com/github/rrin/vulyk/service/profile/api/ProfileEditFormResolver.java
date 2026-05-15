package com.github.rrin.vulyk.service.profile.api;

import com.github.rrin.vulyk.dto.user.UserProfileResponse;

public interface ProfileEditFormResolver {

    String resolveAction(UserProfileResponse profile);

    default String resolvePageKey() {
        return "profile-edit";
    }
}
