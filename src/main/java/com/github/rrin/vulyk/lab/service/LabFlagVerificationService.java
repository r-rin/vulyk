package com.github.rrin.vulyk.lab.service;

import com.github.rrin.vulyk.lab.entity.LabFlagEntity;
import com.github.rrin.vulyk.lab.repository.LabFlagRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabFlagVerificationService {

    private final LabFlagRepository labFlagRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LabFlagEntity requireMatchingFlag(String labId, String submittedFlag) {
        List<LabFlagEntity> flags = labFlagRepository.findAllByLabId(labId);

        for (LabFlagEntity candidate : flags) {
            if (matches(candidate, submittedFlag)) {
                upgradeLegacyPlaintextHash(candidate, submittedFlag);
                return candidate;
            }
        }

        return null;
    }

    public String encode(String rawFlag) {
        return passwordEncoder.encode(rawFlag);
    }

    private boolean matches(LabFlagEntity candidate, String submittedFlag) {
        String storedValue = candidate.getFlagHash();
        if (storedValue == null || storedValue.isBlank()) {
            return false;
        }

        if (isEncodedHash(storedValue)) {
            return passwordEncoder.matches(submittedFlag, storedValue);
        }

        return storedValue.equals(submittedFlag);
    }

    private void upgradeLegacyPlaintextHash(LabFlagEntity candidate, String submittedFlag) {
        if (isEncodedHash(candidate.getFlagHash())) {
            return;
        }

        candidate.setFlagHash(passwordEncoder.encode(submittedFlag));
        labFlagRepository.save(candidate);
    }

    private boolean isEncodedHash(String storedValue) {
        return storedValue.startsWith("$2a$")
            || storedValue.startsWith("$2b$")
            || storedValue.startsWith("$2y$");
    }
}