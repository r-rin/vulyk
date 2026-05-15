package com.github.rrin.vulyk.lab.module.api.bola01;

import com.github.rrin.vulyk.config.FileStorageProperties;
import com.github.rrin.vulyk.domain.entity.file.FileAttachmentEntity;
import com.github.rrin.vulyk.dto.file.FileDownload;
import com.github.rrin.vulyk.exception.NotFoundException;
import com.github.rrin.vulyk.exception.ValidationException;
import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.repository.FileAttachmentRepository;
import com.github.rrin.vulyk.repository.PostRepository;
import com.github.rrin.vulyk.repository.UserRepository;
import com.github.rrin.vulyk.service.FileService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Primary
@Service
@ConditionalOnLabEnabled(BolaHiddenAttachmentLab.LAB_ID)
public class VulnerableBolaAttachmentDownloadFileService extends FileService {

    private final FileAttachmentRepository fileAttachmentRepository;
    private final UserRepository userRepository;

    public VulnerableBolaAttachmentDownloadFileService(
        FileStorageProperties properties,
        FileAttachmentRepository fileAttachmentRepository,
        UserRepository userRepository,
        PostRepository postRepository
    ) {
        super(properties, fileAttachmentRepository, userRepository, postRepository);
        this.fileAttachmentRepository = fileAttachmentRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownload download(Long id, String principalEmail) {
        userRepository.findByEmail(principalEmail)
            .orElseThrow(() -> new ValidationException("User not found"));

        FileAttachmentEntity attachment = fileAttachmentRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("File not found"));

        Path path = Paths.get(attachment.getFilePath());
        if (!Files.exists(path) || Files.isDirectory(path)) {
            throw new NotFoundException("File not found");
        }

        try {
            // Vulnerable by design: no ownership/post visibility checks on file object lookup.
            Resource resource = new UrlResource(path.toUri());
            return new FileDownload(resource, attachment.getContentType(), attachment.getOriginalFilename());
        } catch (IOException ex) {
            throw new ValidationException("Failed to read file");
        }
    }
}
