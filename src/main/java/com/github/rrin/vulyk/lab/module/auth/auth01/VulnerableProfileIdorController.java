package com.github.rrin.vulyk.lab.module.auth.auth01;

import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import com.github.rrin.vulyk.dto.user.UpdateProfileRequest;
import com.github.rrin.vulyk.dto.user.UserProfileResponse;
import com.github.rrin.vulyk.exception.InvalidCredentials;
import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.lab.service.LabProgressService;
import com.github.rrin.vulyk.repository.UserRepository;
import com.github.rrin.vulyk.service.UserService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users/profile")
@ConditionalOnLabEnabled(AuthProfileIdorLab.LAB_ID)
public class VulnerableProfileIdorController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final LabProgressService labProgressService;

    @ResponseBody
    @PutMapping("/{username}")
    public UserProfileResponse updateProfileByUsername(
        @AuthenticationPrincipal String principalEmail,
        @PathVariable String username,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        return updateProfileByUsernameInternal(principalEmail, username, request);
    }

    @PostMapping("/{username}")
    public String updateProfileByUsernameForm(
        @AuthenticationPrincipal String principalEmail,
        @PathVariable String username,
        @RequestParam String email,
        @RequestParam(name = "username") String requestedUsername,
        @RequestParam(name = "name", required = false) String name,
        @RequestParam(name = "phoneNumber", required = false) String phoneNumber,
        @RequestParam(name = "bio", required = false) String bio,
        RedirectAttributes redirectAttributes
    ) {
        try {
            UpdateProfileRequest request = new UpdateProfileRequest(
                requestedUsername,
                email,
                name,
                bio,
                phoneNumber
            );
            updateProfileByUsernameInternal(principalEmail, username, request);
            redirectAttributes.addFlashAttribute("notice", "Profile updated.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/web/profile/edit";
    }

    private UserProfileResponse updateProfileByUsernameInternal(
        String principalEmail,
        String username,
        UpdateProfileRequest request
    ) {
        UserEntity actor = userRepository.findByEmail(principalEmail)
            .orElseThrow(() -> new InvalidCredentials("User not found"));

        UserEntity target = userRepository.findByUsername(username)
            .orElseThrow(() -> new InvalidCredentials("User not found"));

        ProfileSnapshot before = ProfileSnapshot.from(target);
        UserProfileResponse updated = userService.updateProfile(target.getEmail(), request);

        if (!Objects.equals(actor.getId(), target.getId())) {
            List<String> changedFields = collectChangedFields(before, updated);
            if (!changedFields.isEmpty()) {
                String evidence = "actor=" + actor.getUsername()
                    + ":target=" + target.getUsername()
                    + ":fields=" + String.join(",", changedFields);
                labProgressService.completeStateTask(AuthProfileIdorLab.LAB_ID, AuthProfileIdorLab.TASK_ID, evidence);
            }
        }

        return updated;
    }

    private List<String> collectChangedFields(ProfileSnapshot before, UserProfileResponse after) {
        List<String> changed = new ArrayList<>();

        if (!Objects.equals(before.username(), after.getUsername())) {
            changed.add("username");
        }
        if (!Objects.equals(before.email(), after.getEmail())) {
            changed.add("email");
        }
        if (!Objects.equals(before.name(), after.getName())) {
            changed.add("name");
        }
        if (!Objects.equals(before.bio(), after.getBio())) {
            changed.add("bio");
        }
        if (!Objects.equals(before.phoneNumber(), after.getPhoneNumber())) {
            changed.add("phoneNumber");
        }

        return changed;
    }

    private record ProfileSnapshot(String username, String email, String name, String bio, String phoneNumber) {

        private static ProfileSnapshot from(UserEntity user) {
            return new ProfileSnapshot(
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                user.getBio(),
                user.getPhoneNumber()
            );
        }
    }
}
