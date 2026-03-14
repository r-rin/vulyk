package com.github.rrin.vulyk.service;

import com.github.rrin.vulyk.config.FileStorageProperties;
import com.github.rrin.vulyk.domain.entity.file.FileAttachmentEntity;
import com.github.rrin.vulyk.domain.entity.post.PostEntity;
import com.github.rrin.vulyk.domain.entity.post.PostState;
import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import com.github.rrin.vulyk.dto.file.FileAttachmentResponse;
import com.github.rrin.vulyk.dto.file.FileDownload;
import com.github.rrin.vulyk.dto.file.FileUploadResponse;
import com.github.rrin.vulyk.exception.NotFoundException;
import com.github.rrin.vulyk.exception.ValidationException;
import com.github.rrin.vulyk.repository.FileAttachmentRepository;
import com.github.rrin.vulyk.repository.PostRepository;
import com.github.rrin.vulyk.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/png",
        "image/jpeg",
        "image/gif",
        "application/pdf",
        "text/plain"
    );

    private final FileStorageProperties properties;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    private Path profileDir;
    private Path postDir;

    @PostConstruct
    void initDirs() {
        Path base = Paths.get(properties.getLocation()).toAbsolutePath().normalize();
        profileDir = base.resolve(properties.getProfilePictures()).normalize();
        postDir = base.resolve(properties.getPostAttachments()).normalize();
        try {
            Files.createDirectories(profileDir);
            Files.createDirectories(postDir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize file storage directories", e);
        }
    }

    @Transactional
    public FileUploadResponse uploadProfile(String principalEmail, MultipartFile file) {
        UserEntity user = requireUser(principalEmail);
        removeExistingProfilePicture(user);
        FileAttachmentEntity saved = storeFile(file, user, null, profileDir);
        user.setProfilePicture(saved);
        userRepository.save(user);
        return toResponse(saved);
    }

    @Transactional
    public FileUploadResponse uploadPostAttachment(Long postId, String principalEmail, MultipartFile file) {
        UserEntity user = requireUser(principalEmail);
        PostEntity post = postRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException("Post not found"));
        if (post.getAuthor() == null || !post.getAuthor().getEmail().equalsIgnoreCase(principalEmail)) {
            throw new ValidationException("Only the post author can attach files");
        }
        Path postSpecificDir = ensureDir(postDir.resolve(String.valueOf(postId)));
        FileAttachmentEntity saved = storeFile(file, user, post, postSpecificDir);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public FileDownload download(Long id, String principalEmail) {
        UserEntity requester = requireUser(principalEmail);
        FileAttachmentEntity attachment = fileAttachmentRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("File not found"));

        enforceDownloadAccess(attachment, requester);

        Path path = Paths.get(attachment.getFilePath());
        if (!Files.exists(path)) {
            throw new NotFoundException("File not found");
        }
        try {
            Resource resource = new UrlResource(path.toUri());
            return new FileDownload(resource, attachment.getContentType(), attachment.getOriginalFilename());
        } catch (IOException e) {
            throw new ValidationException("Failed to read file");
        }
    }

    @Transactional(readOnly = true)
    public Page<FileAttachmentResponse> listPostAttachments(Long postId, String principalEmail, Pageable pageable) {
        PostEntity post = postRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException("Post not found"));

        UserEntity requester = requireUser(principalEmail);
        if (post.getAuthor() == null || !post.getAuthor().getEmail().equalsIgnoreCase(requester.getEmail())) {
            if (post.getState() == null || !post.getState().equals(PostState.PUBLISHED)) {
                throw new ValidationException("Access denied to attachments for this post");
            }
        }

        return fileAttachmentRepository.findAllByPostId(postId, pageable)
            .map(this::toAttachmentResponse);
    }

    @Transactional(readOnly = true)
    public Page<FileAttachmentResponse> listProfileUploads(String principalEmail, Pageable pageable) {
        UserEntity user = requireUser(principalEmail);
        return fileAttachmentRepository.findAllByUploaderIdAndPostIsNull(user.getId(), pageable)
            .map(this::toAttachmentResponse);
    }

    @Transactional
    public void deleteAttachment(Long id, String principalEmail) {
        UserEntity requester = requireUser(principalEmail);
        FileAttachmentEntity attachment = fileAttachmentRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("File not found"));

        ensureDeletePermission(attachment, requester);

        if (requester.getProfilePicture() != null
            && requester.getProfilePicture().getId().equals(attachment.getId())) {
            requester.setProfilePicture(null);
            userRepository.save(requester);
        }

        deletePhysicalFile(attachment.getFilePath());
        fileAttachmentRepository.delete(attachment);
    }

    private FileAttachmentEntity storeFile(MultipartFile file, UserEntity uploader, PostEntity post, Path targetDir) {
        validateFile(file);
        String originalName = sanitizeOriginal(file.getOriginalFilename());
        String storedName = generateStoredName(originalName);
        Path targetPath = targetDir.resolve(storedName).normalize();
        ensureWithinDirectory(targetDir, targetPath);

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ValidationException("Failed to store file");
        }

        FileAttachmentEntity entity = FileAttachmentEntity.builder()
            .originalFilename(originalName)
            .storedFilename(storedName)
            .filePath(targetPath.toString())
            .contentType(file.getContentType())
            .fileSize(file.getSize())
            .uploader(uploader)
            .post(post)
            .build();

        return fileAttachmentRepository.save(entity);
    }

    private void enforceDownloadAccess(FileAttachmentEntity attachment, UserEntity requester) {
        PostEntity post = attachment.getPost();
        if (post == null) {
            if (!attachment.getUploader().getId().equals(requester.getId())) {
                throw new ValidationException("Access denied to this file");
            }
            return;
        }

        boolean isAuthor = post.getAuthor() != null
            && post.getAuthor().getId().equals(requester.getId());
        boolean isUploader = attachment.getUploader() != null
            && attachment.getUploader().getId().equals(requester.getId());

        if (!isAuthor && !isUploader) {
            if (post.getState() == null || !post.getState().equals(PostState.PUBLISHED)) {
                throw new ValidationException("Access denied to this file");
            }
        }
    }

    private void ensureDeletePermission(FileAttachmentEntity attachment, UserEntity requester) {
        PostEntity post = attachment.getPost();
        boolean isUploader = attachment.getUploader() != null
            && attachment.getUploader().getId().equals(requester.getId());
        if (post == null) {
            if (!isUploader) {
                throw new ValidationException("Only the uploader can delete this file");
            }
            return;
        }

        boolean isAuthor = post.getAuthor() != null
            && post.getAuthor().getId().equals(requester.getId());
        if (!isUploader && !isAuthor) {
            throw new ValidationException("Only the uploader or post author can delete this file");
        }
    }

    private void deletePhysicalFile(String pathStr) {
        if (pathStr == null) {
            return;
        }
        try {
            Path path = Paths.get(pathStr);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new ValidationException("Failed to delete file from storage");
        }
    }

    private void removeExistingProfilePicture(UserEntity user) {
        FileAttachmentEntity current = user.getProfilePicture();
        if (current == null) {
            return;
        }
        user.setProfilePicture(null);
        userRepository.save(user);
        deletePhysicalFile(current.getFilePath());
        fileAttachmentRepository.delete(current);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ValidationException("File is empty");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ValidationException("File exceeds size limit");
        }
        if (file.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new ValidationException("Unsupported file type");
        }
    }

    private String sanitizeOriginal(String original) {
        if (original == null) {
            return "file";
        }
        String cleaned = Paths.get(original).getFileName().toString();
        if (cleaned.contains("..")) {
            throw new ValidationException("Invalid file name");
        }
        return cleaned;
    }

    private String generateStoredName(String original) {
        String extension = "";
        int idx = original.lastIndexOf('.')
;        if (idx != -1 && idx < original.length() - 1) {
            extension = original.substring(idx);
        }
        return UUID.randomUUID() + extension;
    }

    private void ensureWithinDirectory(Path directory, Path target) {
        if (!target.normalize().startsWith(directory.normalize())) {
            throw new ValidationException("Invalid file path");
        }
    }

    private Path ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
            return dir.toAbsolutePath().normalize();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize storage directory", e);
        }
    }

    private UserEntity requireUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ValidationException("User not found"));
    }

    private FileAttachmentResponse toAttachmentResponse(FileAttachmentEntity entity) {
        return new FileAttachmentResponse(
            entity.getId(),
            entity.getOriginalFilename(),
            entity.getContentType(),
            entity.getFileSize(),
            entity.getPost() != null ? entity.getPost().getId() : null,
            entity.getUploader() != null ? entity.getUploader().getUsername() : null,
            entity.getCreatedAt()
        );
    }

    private FileUploadResponse toResponse(FileAttachmentEntity entity) {
        return new FileUploadResponse(
            entity.getId(),
            entity.getOriginalFilename(),
            entity.getStoredFilename(),
            entity.getContentType(),
            entity.getFileSize()
        );
    }
}
