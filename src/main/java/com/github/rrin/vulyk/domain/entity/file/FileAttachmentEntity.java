package com.github.rrin.vulyk.domain.entity.file;

import com.github.rrin.vulyk.domain.Identifiable;
import com.github.rrin.vulyk.domain.entity.AuditableEntity;
import com.github.rrin.vulyk.domain.entity.post.PostEntity;
import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "file_attachments", indexes = {
    @Index(name = "idx_file_uploader", columnList = "uploader_id"),
    @Index(name = "idx_file_post", columnList = "post_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FileAttachmentEntity extends AuditableEntity implements Identifiable<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, length = 255, unique = true)
    private String storedFilename;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private UserEntity uploader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private PostEntity post;
}

