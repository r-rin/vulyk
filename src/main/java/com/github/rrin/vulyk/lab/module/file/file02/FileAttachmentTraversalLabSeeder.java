package com.github.rrin.vulyk.lab.module.file.file02;

import com.github.rrin.vulyk.config.FileStorageProperties;
import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.lab.config.LabProperties;
import com.github.rrin.vulyk.lab.entity.LabFlagEntity;
import com.github.rrin.vulyk.lab.repository.LabFlagRepository;
import com.github.rrin.vulyk.lab.service.LabFlagVerificationService;
import com.github.rrin.vulyk.lab.service.LabProgressService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnLabEnabled(FileAttachmentTraversalLab.LAB_ID)
public class FileAttachmentTraversalLabSeeder implements ApplicationRunner {

    private static final String FLAG_FILE_NAME = "flag.txt";
    private static final String LEGACY_FLAG_FILE_NAME = "file-02-flag.txt";

    private final FileAttachmentTraversalLab labDefinition;
    private final FileStorageProperties fileStorageProperties;
    private final LabProperties labProperties;
    private final LabFlagRepository labFlagRepository;
    private final LabFlagVerificationService labFlagVerificationService;
    private final LabProgressService labProgressService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        labProgressService.ensureProgressRows(labDefinition);

        String flag = ensureFlagValue();
        writeSeedFile(flag);
    }

    private String ensureFlagValue() {
        return labFlagRepository.findByLabIdAndTaskId(labDefinition.getId(), FileAttachmentTraversalLab.TASK_READ_FLAG)
            .map(this::upgradeAndResolveFlagValue)
            .orElseGet(() -> {
                String rawFlag = generateFlag();
                labFlagRepository.save(LabFlagEntity.builder()
                    .labId(labDefinition.getId())
                    .taskId(FileAttachmentTraversalLab.TASK_READ_FLAG)
                    .flagHash(labFlagVerificationService.encode(rawFlag))
                    .seedContext(FLAG_FILE_NAME)
                    .build());
                return rawFlag;
            });
    }

    private String upgradeAndResolveFlagValue(LabFlagEntity existing) {
        String storedValue = existing.getFlagHash();
        if (storedValue != null && !storedValue.isBlank() && !storedValue.startsWith("$2")) {
            existing.setFlagHash(labFlagVerificationService.encode(storedValue));
            labFlagRepository.save(existing);
            return storedValue;
        }

        try {
            return readFlagFromSeedFile();
        } catch (IllegalStateException ex) {
            String rawFlag = generateFlag();
            existing.setFlagHash(labFlagVerificationService.encode(rawFlag));
            labFlagRepository.save(existing);
            return rawFlag;
        }
    }

    private String readFlagFromSeedFile() {
        Path flagFile = resolveFlagFilePath();
        if (!Files.exists(flagFile)) {
            Path legacyFlagFile = resolveLegacyFlagFilePath();
            if (Files.exists(legacyFlagFile)) {
                return readFlagValue(legacyFlagFile);
            }

            Path olderLegacyFlagFile = resolveOlderLegacyFlagFilePath();
            if (Files.exists(olderLegacyFlagFile)) {
                return readFlagValue(olderLegacyFlagFile);
            }
            throw new IllegalStateException("Unable to recover seeded FILE-02 flag value");
        }

        return readFlagValue(flagFile);
    }

    private String readFlagValue(Path flagFile) {
        try {
            String content = Files.readString(flagFile);
            String marker = "Training flag: ";
            int markerIndex = content.indexOf(marker);
            if (markerIndex < 0) {
                throw new IllegalStateException("Seeded FILE-02 file does not contain a recoverable flag value");
            }
            return content.substring(markerIndex + marker.length()).trim();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to recover seeded FILE-02 flag value", e);
        }
    }

    private void writeSeedFile(String flag) {
        Path flagFile = resolveFlagFilePath();

        try {
            Files.createDirectories(flagFile.getParent());
            String content = "Attachment index.\n"
                + "Do not expose this file through public download routes.\n"
                + "Training flag: " + flag + "\n";
            Files.writeString(
                flagFile,
                content,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to seed FILE-02 traversal target", e);
        }
    }

    private Path resolveFlagFilePath() {
        Path base = Paths.get(fileStorageProperties.getLocation()).toAbsolutePath().normalize();
        return base.resolve(FLAG_FILE_NAME).normalize();
    }

    private Path resolveLegacyFlagFilePath() {
        Path base = Paths.get(fileStorageProperties.getLocation()).toAbsolutePath().normalize();
        Path postRoot = base.resolve(fileStorageProperties.getPostAttachments()).normalize();
        Path traversalParent = postRoot.getParent();
        if (traversalParent == null) {
            throw new IllegalStateException("Unable to resolve FILE-02 traversal seed path");
        }
        return traversalParent.resolve("protected").resolve(LEGACY_FLAG_FILE_NAME).normalize();
    }

    private Path resolveOlderLegacyFlagFilePath() {
        Path base = Paths.get(fileStorageProperties.getLocation()).toAbsolutePath().normalize();
        return base.resolve("protected").resolve(LEGACY_FLAG_FILE_NAME).normalize();
    }

    private String generateFlag() {
        return labProperties.getValidation().getFlagPrefix()
            + "file-attachment-traversal-"
            + UUID.randomUUID().toString().substring(0, 12)
            + labProperties.getValidation().getFlagSuffix();
    }
}
