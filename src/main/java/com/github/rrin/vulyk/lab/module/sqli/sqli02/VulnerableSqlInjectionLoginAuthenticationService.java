package com.github.rrin.vulyk.lab.module.sqli.sqli02;

import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import com.github.rrin.vulyk.dto.auth.LoginRequest;
import com.github.rrin.vulyk.exception.InvalidCredentials;
import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.lab.service.LabProgressService;
import com.github.rrin.vulyk.repository.UserRepository;
import com.github.rrin.vulyk.service.auth.api.LoginAuthenticationService;
import com.github.rrin.vulyk.service.auth.impl.SecureLoginAuthenticationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
@RequiredArgsConstructor
@ConditionalOnLabEnabled(SqlInjectionLoginLab.LAB_ID)
public class VulnerableSqlInjectionLoginAuthenticationService implements LoginAuthenticationService {

    private final UserRepository userRepository;
    private final SecureLoginAuthenticationService secureLoginAuthenticationService;
    private final LabProgressService labProgressService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public UserEntity authenticate(LoginRequest request) {
        String identifier = request.getIdentifier() == null ? "" : request.getIdentifier();

        boolean suspiciousPayload = looksLikeSqlPayload(identifier);
        if (!suspiciousPayload) {
            return secureLoginAuthenticationService.authenticate(request);
        }

        String sql = "select u.id from users u where u.is_deleted = false"
            + " and (lower(u.username) = lower('" + identifier + "') or lower(u.email) = lower('" + identifier + "'))"
            + " order by u.id asc";

        List<Number> rows;
        try {
            List<Number> result = entityManager.createNativeQuery(sql)
                .setMaxResults(1)
                .getResultList();
            rows = result;
        } catch (RuntimeException ex) {
            throw new InvalidCredentials("Invalid email or password");
        }

        if (rows.isEmpty()) {
            throw new InvalidCredentials("Invalid email or password");
        }

        Long userId = rows.get(0).longValue();
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new InvalidCredentials("Invalid email or password"));

        String evidence = "identifier-bypass:userId=" + userId + ":i=" + compactHash(identifier);
        labProgressService.completeStateTask(
            SqlInjectionLoginLab.LAB_ID,
            SqlInjectionLoginLab.TASK_DISCOVER_ID,
            evidence
        );

        return user;
    }

    private boolean looksLikeSqlPayload(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("'")
            || normalized.contains("\"")
            || normalized.contains("--")
            || normalized.contains("/*")
            || normalized.contains("*/")
            || normalized.contains(";")
            || normalized.contains(" or ")
            || normalized.contains(" and ");
    }

    private String compactHash(String value) {
        int hash = value == null ? 0 : value.hashCode();
        return Integer.toHexString(hash);
    }
}
