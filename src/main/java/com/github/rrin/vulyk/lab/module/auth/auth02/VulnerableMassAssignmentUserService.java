package com.github.rrin.vulyk.lab.module.auth.auth02;

import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import com.github.rrin.vulyk.domain.entity.user.UserRole;
import com.github.rrin.vulyk.dto.user.UpdateProfileRequest;
import com.github.rrin.vulyk.dto.user.UserProfileResponse;
import com.github.rrin.vulyk.exception.ValidationException;
import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.lab.service.LabProgressService;
import com.github.rrin.vulyk.repository.UserRepository;
import com.github.rrin.vulyk.security.JwtTokenProvider;
import com.github.rrin.vulyk.service.UserService;
import com.github.rrin.vulyk.service.auth.api.LoginAuthenticationService;
import java.util.Locale;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Primary
@Service
@ConditionalOnLabEnabled(AuthMassAssignmentLab.LAB_ID)
public class VulnerableMassAssignmentUserService extends UserService {

    private final UserRepository userRepository;
    private final LabProgressService labProgressService;

    public VulnerableMassAssignmentUserService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider tokenProvider,
        LoginAuthenticationService loginAuthenticationService,
        LabProgressService labProgressService
    ) {
        super(userRepository, passwordEncoder, tokenProvider, loginAuthenticationService);
        this.userRepository = userRepository;
        this.labProgressService = labProgressService;
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String principalEmail, UpdateProfileRequest request) {
        UserEntity actor = userRepository.findByEmail(principalEmail)
            .orElseThrow(() -> new ValidationException("User not found"));

        UserRole previousRole = actor.getRole();
        super.updateProfile(principalEmail, request);

        String requestedRole = request == null ? null : request.getRole();
        if (requestedRole != null && !requestedRole.isBlank()) {
            UserRole nextRole;
            try {
                nextRole = UserRole.valueOf(requestedRole.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new ValidationException("Unsupported role value");
            }

            // Vulnerable by design: role is over-posted from profile update payload.
            actor.setRole(nextRole);
            userRepository.save(actor);

            if (previousRole != UserRole.ADMIN && nextRole == UserRole.ADMIN) {
                String evidence = "actor=" + actor.getUsername() + ":role=" + nextRole.name();
                labProgressService.completeStateTask(
                    AuthMassAssignmentLab.LAB_ID,
                    AuthMassAssignmentLab.TASK_INJECT_ID,
                    evidence
                );
            }
        }

        return toProfile(actor);
    }

    private UserProfileResponse toProfile(UserEntity user) {
        return new UserProfileResponse(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getBio(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getProfilePicture() != null ? user.getProfilePicture().getId() : null
        );
    }
}
