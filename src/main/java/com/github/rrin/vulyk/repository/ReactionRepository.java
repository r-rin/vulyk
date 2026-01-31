package com.github.rrin.vulyk.repository;

import com.github.rrin.vulyk.domain.entity.reaction.ReactionEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReactionRepository extends JpaRepository<ReactionEntity, Long> {

    Optional<ReactionEntity> findByUserIdAndPostId(Long userId, Long postId);

    Optional<ReactionEntity> findByUserIdAndCommentId(Long userId, Long commentId);

    long countByPostId(Long postId);

    long countByCommentId(Long commentId);

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    Page<ReactionEntity> findAllByUserIdAndPostIsNotNull(Long userId, Pageable pageable);

    Page<ReactionEntity> findAllByUserIdAndCommentIsNotNull(Long userId, Pageable pageable);
}
