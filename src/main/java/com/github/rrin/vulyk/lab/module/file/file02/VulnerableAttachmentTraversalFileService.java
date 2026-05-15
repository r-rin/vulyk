package com.github.rrin.vulyk.lab.module.file.file02;

import com.github.rrin.vulyk.config.FileStorageProperties;
import com.github.rrin.vulyk.domain.entity.file.FileAttachmentEntity;
import com.github.rrin.vulyk.domain.entity.post.PostEntity;
import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import com.github.rrin.vulyk.dto.file.FileUploadResponse;
import com.github.rrin.vulyk.exception.NotFoundException;
import com.github.rrin.vulyk.exception.ValidationException;
import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.lab.service.LabProgressService;
import com.github.rrin.vulyk.repository.FileAttachmentRepository;
import com.github.rrin.vulyk.repository.PostRepository;
import com.github.rrin.vulyk.repository.UserRepository;
import com.github.rrin.vulyk.service.FileService;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Primary
@Service
@ConditionalOnLabEnabled(FileAttachmentTraversalLab.LAB_ID)
public class VulnerableAttachmentTraversalFileService extends FileService {

    private final FileStorageProperties properties;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final LabProgressService labProgressService;

    public VulnerableAttachmentTraversalFileService(
        FileStorageProperties properties,
        FileAttachmentRepository fileAttachmentRepository,
        UserRepository userRepository,
        PostRepository postRepository,
        LabProgressService labProgressService
    ) {
        super(properties, fileAttachmentRepository, userRepository, postRepository);
        this.properties = properties;
        this.fileAttachmentRepository = fileAttachmentRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.labProgressService = labProgressService;
    }

    @Override
    @Transactional
    public FileUploadResponse uploadPostAttachment(Long postId, String principalEmail, MultipartFile file) {
        String originalName = file == null ? null : file.getOriginalFilename();
        if (!looksLikeTraversal(originalName)) {
            return super.uploadPostAttachment(postId, principalEmail, file);
        }

        if (file == null || file.isEmpty()) {
            throw new ValidationException("File is empty");
        }

        UserEntity user = userRepository.findByEmail(principalEmail)
            .orElseThrow(() -> new ValidationException("User not found"));

        PostEntity post = postRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException("Post not found"));

        if (post.getAuthor() == null || !post.getAuthor().getEmail().equalsIgnoreCase(principalEmail)) {
            throw new ValidationException("Only the post author can attach files");
        }

        Path base = Paths.get(properties.getLocation()).toAbsolutePath().normalize();
        Path postRoot = base.resolve(properties.getPostAttachments()).normalize();
        Path postDir = postRoot.resolve(String.valueOf(postId)).normalize();
        Path target = postDir.resolve(originalName).normalize();

        try {
            Files.createDirectories(postDir);
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }

            try {
                Files.copy(file.getInputStream(), target);
            } catch (FileAlreadyExistsException ignored) {
                // Vulnerable by design: keep pointing metadata at an existing traversal target.
            }
        } catch (IOException ex) {
            throw new ValidationException("Failed to store file");
        }

        long fileSize;
        try {
            fileSize = Files.size(target);
        } catch (IOException ex) {
            throw new ValidationException("Failed to size file");
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        FileAttachmentEntity saved = fileAttachmentRepository.save(FileAttachmentEntity.builder()
            .originalFilename(originalName)
            .storedFilename(generateStoredFilename(originalName))
            .filePath(target.toString())
            .contentType(contentType)
            .fileSize(fileSize)
            .uploader(user)
            .post(post)
            .build());

        labProgressService.completeStateTask(
            FileAttachmentTraversalLab.LAB_ID,
            FileAttachmentTraversalLab.TASK_FIND_VECTOR,
            "filename=" + originalName + ":target=" + target
        );

        return new FileUploadResponse(
            saved.getId(),
            saved.getOriginalFilename(),
            saved.getStoredFilename(),
            saved.getContentType(),
            saved.getFileSize()
        );
    }

    private boolean looksLikeTraversal(String filename) {
        if (filename == null || filename.isBlank()) {
            return false;
        }

        return filename.contains("../") || filename.contains("..\\") || filename.contains("..%2f") || filename.contains("..%5c");
    }

    private String generateStoredFilename(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return UUID.randomUUID().toString();
        }

        int lastDot = originalName.lastIndexOf('.');
        String extension = "";
        if (lastDot >= 0 && lastDot < originalName.length() - 1) {
            extension = originalName.substring(lastDot);
        }

        return UUID.randomUUID() + extension;
    }
}
