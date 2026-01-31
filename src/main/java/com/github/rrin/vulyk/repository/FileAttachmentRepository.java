package com.github.rrin.vulyk.repository;

import com.github.rrin.vulyk.domain.entity.file.FileAttachmentEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachmentEntity, Long> {

    List<FileAttachmentEntity> findAllByPostId(Long postId);

    Page<FileAttachmentEntity> findAllByUploaderId(Long uploaderId, Pageable pageable);

    Optional<FileAttachmentEntity> findByStoredFilename(String storedFilename);
}
