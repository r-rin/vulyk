package com.github.rrin.vulyk.dto.file;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileAttachmentResponse {
    private Long id;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private Long postId;
    private String uploaderUsername;
    private Instant createdAt;
}
