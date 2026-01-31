package com.github.rrin.vulyk.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.file-storage")
public class FileStorageProperties {
    /**
     * Base directory for uploaded files.
     */
    private String location;

    /**
     * Directory for profile pictures.
     */
    private String profilePictures;

    /**
     * Directory for post attachments.
     */
    private String postAttachments;
}
