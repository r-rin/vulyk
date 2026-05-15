package com.github.rrin.vulyk.lab.module.api.bola01;

import com.github.rrin.vulyk.config.FileStorageProperties;
import com.github.rrin.vulyk.domain.entity.file.FileAttachmentEntity;
import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import com.github.rrin.vulyk.domain.entity.user.UserRole;
import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.lab.config.LabProperties;
import com.github.rrin.vulyk.lab.entity.LabFlagEntity;
import com.github.rrin.vulyk.lab.repository.LabFlagRepository;
import com.github.rrin.vulyk.lab.service.LabFlagVerificationService;
import com.github.rrin.vulyk.lab.service.LabProgressService;
import com.github.rrin.vulyk.repository.FileAttachmentRepository;
import com.github.rrin.vulyk.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnLabEnabled(BolaHiddenAttachmentLab.LAB_ID)
public class BolaHiddenAttachmentLabSeeder implements ApplicationRunner {

    private static final String OWNER_USERNAME = "bola-hidden-owner";
    private static final String LEGACY_STORED_FILENAME = "bola-01-hidden-attachment.txt";
    private static final String STORED_FILENAME_PREFIX = "bola-01-hidden-attachment-";
    private static final int ATTACHMENT_POOL_SIZE = 30;
    private static final String FLAG_MARKER = "Training flag: ";

    private final BolaHiddenAttachmentLab labDefinition;
    private final FileStorageProperties fileStorageProperties;
    private final LabProperties labProperties;
    private final LabFlagRepository labFlagRepository;
    private final LabFlagVerificationService labFlagVerificationService;
    private final LabProgressService labProgressService;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        labProgressService.ensureProgressRows(labDefinition);

        String flag = ensureFlagValue();
        UserEntity owner = ensureOwner();
        seedAttachmentPool(owner, flag);
        removeLegacySeededAttachment();
    }

    private String ensureFlagValue() {
        return labFlagRepository.findByLabIdAndTaskId(labDefinition.getId(), BolaHiddenAttachmentLab.TASK_ID)
            .map(this::upgradeAndResolveFlagValue)
            .orElseGet(() -> {
                String rawFlag = generateFlag();
                labFlagRepository.save(LabFlagEntity.builder()
                    .labId(labDefinition.getId())
                    .taskId(BolaHiddenAttachmentLab.TASK_ID)
                    .flagHash(labFlagVerificationService.encode(rawFlag))
                    .seedContext("file_attachments.pool_size=" + ATTACHMENT_POOL_SIZE)
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
            return readFlagFromSeededAttachments();
        } catch (IllegalStateException ex) {
            String rawFlag = generateFlag();
            existing.setFlagHash(labFlagVerificationService.encode(rawFlag));
            labFlagRepository.save(existing);
            return rawFlag;
        }
    }

    private UserEntity ensureOwner() {
        UserEntity owner = userRepository.findByUsername(OWNER_USERNAME)
            .orElseGet(() -> UserEntity.builder()
                .username(OWNER_USERNAME)
                .email(OWNER_USERNAME + "@vulyk.lab")
                .passwordHash(passwordEncoder.encode("bola-seeded-password"))
                .role(UserRole.USER)
                .build());

        owner.setName("Hidden Attachment Owner");
        owner.setBio("Maintains private attachment vault records.");
        return userRepository.save(owner);
    }

    private String readFlagFromSeededAttachments() {
        List<FileAttachmentEntity> attachments = fileAttachmentRepository.findAll();
        for (FileAttachmentEntity attachment : attachments) {
            String storedFilename = attachment.getStoredFilename();
            if (!isSeededFilename(storedFilename) && !LEGACY_STORED_FILENAME.equals(storedFilename)) {
                continue;
            }

            String recoveredFlag = extractFlagIfPresent(Path.of(attachment.getFilePath()));
            if (recoveredFlag != null) {
                return recoveredFlag;
            }
        }

        String recoveredLegacyFlag = extractFlagIfPresent(resolveAttachmentPath(LEGACY_STORED_FILENAME));
        if (recoveredLegacyFlag != null) {
            return recoveredLegacyFlag;
        }

        throw new IllegalStateException("Unable to recover seeded BOLA-01 flag value");
    }

    private String extractFlagIfPresent(Path attachmentPath) {
        if (attachmentPath == null || !Files.exists(attachmentPath) || Files.isDirectory(attachmentPath)) {
            return null;
        }

        try {
            String content = Files.readString(attachmentPath);
            int markerIndex = content.indexOf(FLAG_MARKER);
            if (markerIndex < 0) {
                return null;
            }
            return content.substring(markerIndex + FLAG_MARKER.length()).trim();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to recover seeded BOLA-01 flag value", e);
        }
    }

    private void seedAttachmentPool(UserEntity owner, String flag) {
        int flaggedSlot = secureRandom.nextInt(ATTACHMENT_POOL_SIZE) + 1;
        Set<String> expectedStoredFilenames = new HashSet<>();

        for (int slot = 1; slot <= ATTACHMENT_POOL_SIZE; slot++) {
            String storedFilename = buildStoredFilename(slot);
            expectedStoredFilenames.add(storedFilename);

            Path attachmentPath = resolveAttachmentPath(storedFilename);
            String attachmentFlag = slot == flaggedSlot ? flag : null;
            writeAttachmentFile(attachmentPath, slot, attachmentFlag);
            ensureAttachmentRow(owner, attachmentPath, storedFilename, slot);
        }

        removeUnexpectedSeededAttachments(expectedStoredFilenames);

        labFlagRepository.findByLabIdAndTaskId(labDefinition.getId(), BolaHiddenAttachmentLab.TASK_ID)
            .ifPresent(flagEntity -> {
                flagEntity.setSeedContext("file_attachments.pool_size=" + ATTACHMENT_POOL_SIZE + ",flag_slot=" + flaggedSlot);
                labFlagRepository.save(flagEntity);
            });
    }

    private void writeAttachmentFile(Path attachmentPath, int slot, String flag) {
        try {
            Files.createDirectories(attachmentPath.getParent());
            Files.writeString(
                attachmentPath,
                buildAttachmentContent(slot, flag),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to seed BOLA-01 attachment file", e);
        }
    }

    private String buildAttachmentContent(int slot, String flag) {
        if (flag != null) {
            return "Private attachment vault record #" + slot + ".\n"
                + "Do not expose via object-id lookups.\n"
                + FLAG_MARKER + flag + "\n";
        }

        return "Private attachment vault record #" + slot + ".\n"
            + "Do not expose via object-id lookups.\n"
            + "No challenge material in this object.\n"
            + "Reference token: " + UUID.randomUUID() + "\n";
    }

    private void ensureAttachmentRow(UserEntity owner, Path attachmentPath, String storedFilename, int slot) {
        FileAttachmentEntity attachment = fileAttachmentRepository.findByStoredFilename(storedFilename)
            .orElseGet(() -> FileAttachmentEntity.builder()
                .storedFilename(storedFilename)
                .build());

        attachment.setOriginalFilename("internal-note-" + slot + ".txt");
        attachment.setFilePath(attachmentPath.toString());
        attachment.setContentType("text/plain");
        try {
            attachment.setFileSize(Files.size(attachmentPath));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to size BOLA-01 attachment file", e);
        }
        attachment.setUploader(owner);
        attachment.setPost(null);
        fileAttachmentRepository.save(attachment);
    }

    private void removeUnexpectedSeededAttachments(Set<String> expectedStoredFilenames) {
        List<FileAttachmentEntity> attachments = fileAttachmentRepository.findAll();
        for (FileAttachmentEntity attachment : attachments) {
            String storedFilename = attachment.getStoredFilename();
            if (!isSeededFilename(storedFilename) || expectedStoredFilenames.contains(storedFilename)) {
                continue;
            }

            deleteAttachmentFile(Path.of(attachment.getFilePath()));
            fileAttachmentRepository.delete(attachment);
        }
    }

    private void removeLegacySeededAttachment() {
        fileAttachmentRepository.findByStoredFilename(LEGACY_STORED_FILENAME)
            .ifPresent(attachment -> {
                deleteAttachmentFile(Path.of(attachment.getFilePath()));
                fileAttachmentRepository.delete(attachment);
            });
    }

    private void deleteAttachmentFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to clean old BOLA-01 seeded attachment", e);
        }
    }

    private String buildStoredFilename(int slot) {
        return STORED_FILENAME_PREFIX + String.format("%02d", slot) + ".txt";
    }

    private boolean isSeededFilename(String storedFilename) {
        return storedFilename != null
            && storedFilename.startsWith(STORED_FILENAME_PREFIX)
            && storedFilename.endsWith(".txt");
    }

    private Path resolveAttachmentPath(String storedFilename) {
        Path base = Paths.get(fileStorageProperties.getLocation()).toAbsolutePath().normalize();
        return base.resolve(fileStorageProperties.getPostAttachments())
            .resolve("private")
            .resolve(storedFilename)
            .normalize();
    }

    private String generateFlag() {
        return labProperties.getValidation().getFlagPrefix()
            + "bola-hidden-attachment-"
            + UUID.randomUUID().toString().substring(0, 12)
            + labProperties.getValidation().getFlagSuffix();
    }
}