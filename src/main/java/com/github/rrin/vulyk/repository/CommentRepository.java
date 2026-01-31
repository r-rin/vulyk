package com.github.rrin.vulyk.repository;

import com.github.rrin.vulyk.domain.entity.comment.CommentEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    Page<CommentEntity> findAllByPostId(Long postId, Pageable pageable);

    Page<CommentEntity> findAllByPostIdAndParentCommentIsNull(Long postId, Pageable pageable);

    Page<CommentEntity> findAllByParentCommentId(Long parentCommentId, Pageable pageable);

    Optional<CommentEntity> findByIdAndAuthorId(Long id, Long authorId);
}
