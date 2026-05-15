package com.github.rrin.vulyk.lab.module.file.file01;

import com.github.rrin.vulyk.config.FileStorageProperties;
import com.github.rrin.vulyk.exception.NotFoundException;
import com.github.rrin.vulyk.exception.ValidationException;
import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/files/profile-images")
@ConditionalOnLabEnabled(FileProfilePathTraversalLab.LAB_ID)
public class VulnerableProfileImageTraversalController {

    private final FileStorageProperties fileStorageProperties;

    private Path profileDir;

    @PostConstruct
    void init() {
        Path base = Paths.get(fileStorageProperties.getLocation()).toAbsolutePath().normalize();
        profileDir = base.resolve(fileStorageProperties.getProfilePictures()).normalize();
        try {
            Files.createDirectories(profileDir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize profile image directory", e);
        }
    }

    @GetMapping("/view")
    public ResponseEntity<Resource> view(@RequestParam("name") String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("name is required");
        }

        Path target = profileDir.resolve(name).normalize();
        if (!Files.exists(target) || Files.isDirectory(target)) {
            throw new NotFoundException("Profile image not found");
        }

        try {
            Resource resource = new UrlResource(target.toUri());
            String contentType = Files.probeContentType(target);
            MediaType mediaType = contentType == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(contentType);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + target.getFileName() + "\"")
                .contentType(mediaType)
                .body(resource);
        } catch (IOException | IllegalArgumentException e) {
            throw new ValidationException("Failed to read file");
        }
    }
}
