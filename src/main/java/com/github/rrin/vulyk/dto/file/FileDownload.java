package com.github.rrin.vulyk.dto.file;

import org.springframework.core.io.Resource;

public class FileDownload {
    private final Resource resource;
    private final String contentType;
    private final String originalFilename;

    public FileDownload(Resource resource, String contentType, String originalFilename) {
        this.resource = resource;
        this.contentType = contentType;
        this.originalFilename = originalFilename;
    }

    public Resource getResource() {
        return resource;
    }

    public String getContentType() {
        return contentType;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }
}
