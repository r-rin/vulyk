package com.github.rrin.vulyk.lab.module.file.file01;

import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.repository.FileAttachmentRepository;
import com.github.rrin.vulyk.service.file.api.ProfileImageUrlResolver;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
@RequiredArgsConstructor
@ConditionalOnLabEnabled(FileProfilePathTraversalLab.LAB_ID)
public class VulnerableProfileImageUrlResolver implements ProfileImageUrlResolver {

    private final FileAttachmentRepository fileAttachmentRepository;

    @Override
    public String resolve(Long profilePictureId) {
        if (profilePictureId == null) {
            return null;
        }

        return fileAttachmentRepository.findById(profilePictureId)
            .map(attachment -> attachment.getStoredFilename())
            .filter(filename -> filename != null && !filename.isBlank())
            .map(filename -> "/files/profile-images/view?name="
                + URLEncoder.encode(filename, StandardCharsets.UTF_8))
            .orElse("/files/" + profilePictureId);
    }
}
