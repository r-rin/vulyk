package com.github.rrin.vulyk.lab.module.sqli.sqli02;

import com.github.rrin.vulyk.domain.entity.post.PostEntity;
import com.github.rrin.vulyk.domain.entity.post.PostState;
import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import com.github.rrin.vulyk.domain.entity.user.UserRole;
import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.lab.config.LabProperties;
import com.github.rrin.vulyk.lab.entity.LabFlagEntity;
import com.github.rrin.vulyk.lab.repository.LabFlagRepository;
import com.github.rrin.vulyk.lab.service.LabFlagVerificationService;
import com.github.rrin.vulyk.lab.service.LabProgressService;
import com.github.rrin.vulyk.repository.PostRepository;
import com.github.rrin.vulyk.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnLabEnabled(SqlInjectionLoginLab.LAB_ID)
public class SqlInjectionLoginLabSeeder implements ApplicationRunner {

    private static final String ADMIN_USERNAME = "super-admin";
    private static final String VISIBLE_POST_TITLE = "Incident Replay Briefing";
    private static final String DRAFT_FLAG_POST_TITLE = "Auth Oracle Recovery Draft";

    private final SqlInjectionLoginLab labDefinition;
    private final LabProperties labProperties;
    private final LabFlagRepository labFlagRepository;
    private final LabFlagVerificationService labFlagVerificationService;
    private final LabProgressService labProgressService;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        labProgressService.ensureProgressRows(labDefinition);

        UserEntity admin = ensureUser(
            ADMIN_USERNAME,
            "oracle-auditor@vulyk.lab",
            "Oracle Auditor",
            "Reviews restricted authentication telemetry and account recovery workflows."
        );

        String visibleFlag = ensureFlagValue(admin);

        ensurePost(
            admin,
            VISIBLE_POST_TITLE,
            "Public briefing from oracle-auditor on authentication incident replay procedures.",
            PostState.PUBLISHED
        );

        ensurePost(
            admin,
            DRAFT_FLAG_POST_TITLE,
            "Restricted draft notes for SQLI-02. Training flag: " + visibleFlag,
            PostState.DRAFT
        );
    }

    private UserEntity ensureUser(String username, String email, String name, String bio) {
        return userRepository.findByEmail(email)
            .orElseGet(() -> userRepository.save(UserEntity.builder()
                .username(username)
                .email(email)
                .name(name)
                .bio(bio)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(UserRole.USER)
                .build()));
    }

    private String ensureFlagValue(UserEntity admin) {
        return labFlagRepository.findByLabIdAndTaskId(labDefinition.getId(), SqlInjectionLoginLab.TASK_RECOVER_ID)
            .map(existing -> upgradeAndResolveFlagValue(existing, admin))
            .orElseGet(() -> {
                String rawFlag = generateFlag();
                labFlagRepository.save(LabFlagEntity.builder()
                    .labId(labDefinition.getId())
                    .taskId(SqlInjectionLoginLab.TASK_RECOVER_ID)
                    .flagHash(labFlagVerificationService.encode(rawFlag))
                    .seedContext("posts.content")
                    .build());
                return rawFlag;
            });
    }

    private String upgradeAndResolveFlagValue(LabFlagEntity existing, UserEntity admin) {
        String storedValue = existing.getFlagHash();
        if (storedValue != null && !storedValue.isBlank() && !storedValue.startsWith("$2")) {
            existing.setFlagHash(labFlagVerificationService.encode(storedValue));
            labFlagRepository.save(existing);
            return storedValue;
        }

        try {
            return postRepository.findByAuthorIdAndTitleIgnoreCase(admin.getId(), DRAFT_FLAG_POST_TITLE)
                .map(PostEntity::getContent)
                .map(this::extractFlagValue)
                .orElseThrow(() -> new IllegalStateException("Unable to recover seeded flag value for SQLI-02"));
        } catch (IllegalStateException ex) {
            String rawFlag = generateFlag();
            existing.setFlagHash(labFlagVerificationService.encode(rawFlag));
            labFlagRepository.save(existing);
            return rawFlag;
        }
    }

    private void ensurePost(
        UserEntity author,
        String title,
        String content,
        PostState state
    ) {
        PostEntity entity = postRepository.findByAuthorIdAndTitleIgnoreCase(author.getId(), title)
            .orElseGet(() -> PostEntity.builder()
                .author(author)
                .title(title)
                .build());

        entity.setContent(content);
        entity.setState(state);
        postRepository.save(entity);
    }

    private String generateFlag() {
        return labProperties.getValidation().getFlagPrefix()
            + "sqli-login-oracle-"
            + UUID.randomUUID().toString().substring(0, 12)
            + labProperties.getValidation().getFlagSuffix();
    }

    private String extractFlagValue(String description) {
        String marker = "Training flag: ";
        int markerIndex = description == null ? -1 : description.indexOf(marker);
        if (markerIndex < 0) {
            throw new IllegalStateException("Seeded SQLI-02 draft post no longer contains a recoverable flag value");
        }

        return description.substring(markerIndex + marker.length()).trim();
    }
}
