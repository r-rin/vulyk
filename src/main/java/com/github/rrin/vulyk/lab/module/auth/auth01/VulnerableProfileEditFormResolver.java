package com.github.rrin.vulyk.lab.module.auth.auth01;

import com.github.rrin.vulyk.dto.user.UserProfileResponse;
import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.service.profile.api.ProfileEditFormResolver;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
@ConditionalOnLabEnabled(AuthProfileIdorLab.LAB_ID)
public class VulnerableProfileEditFormResolver implements ProfileEditFormResolver {

    @Override
    public String resolveAction(UserProfileResponse profile) {
        String username = profile == null || profile.getUsername() == null
            ? ""
            : profile.getUsername().trim();
        return "/users/profile/" + URLEncoder.encode(username, StandardCharsets.UTF_8);
    }

    @Override
    public String resolvePageKey() {
        return "profile-edit-form";
    }
}
