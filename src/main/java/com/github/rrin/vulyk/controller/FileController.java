package com.github.rrin.vulyk.controller;

import com.github.rrin.vulyk.dto.file.FileAttachmentResponse;
import com.github.rrin.vulyk.dto.file.FileDownload;
import com.github.rrin.vulyk.dto.file.FileUploadResponse;
import com.github.rrin.vulyk.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/profile")
    @ResponseStatus(HttpStatus.CREATED)
    public FileUploadResponse uploadProfile(
        @AuthenticationPrincipal String principalEmail,
        @RequestParam("file") MultipartFile file
    ) {
        return fileService.uploadProfile(principalEmail, file);
    }

    @PostMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.CREATED)
    public FileUploadResponse uploadPostAttachment(
        @PathVariable Long postId,
        @AuthenticationPrincipal String principalEmail,
        @RequestParam("file") MultipartFile file
    ) {
        return fileService.uploadPostAttachment(postId, principalEmail, file);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(
        @PathVariable Long id,
        @AuthenticationPrincipal String principalEmail
    ) {
        FileDownload download = fileService.download(id, principalEmail);
        Resource resource = download.getResource();
        MediaType mediaType;
        try {
            mediaType = download.getContentType() != null
                ? MediaType.parseMediaType(download.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        } catch (IllegalArgumentException ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        String filename = download.getOriginalFilename() != null
            ? download.getOriginalFilename()
            : resource.getFilename();

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
            .contentType(mediaType)
            .body(resource);
    }

    @GetMapping("/posts/{postId}")
    public Page<FileAttachmentResponse> listPostAttachments(
        @PathVariable Long postId,
        @AuthenticationPrincipal String principalEmail,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return fileService.listPostAttachments(postId, principalEmail, pageable);
    }

    @GetMapping("/profile")
    public Page<FileAttachmentResponse> listProfileUploads(
        @AuthenticationPrincipal String principalEmail,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return fileService.listProfileUploads(principalEmail, pageable);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable Long id,
        @AuthenticationPrincipal String principalEmail
    ) {
        fileService.deleteAttachment(id, principalEmail);
    }
}
