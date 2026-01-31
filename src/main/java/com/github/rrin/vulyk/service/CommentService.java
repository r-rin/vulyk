package com.github.rrin.vulyk.service;

import com.github.rrin.vulyk.domain.entity.comment.CommentEntity;
import com.github.rrin.vulyk.domain.entity.post.PostEntity;
import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import com.github.rrin.vulyk.dto.comment.CommentRequest;
import com.github.rrin.vulyk.dto.comment.CommentResponse;
import com.github.rrin.vulyk.exception.NotFoundException;
import com.github.rrin.vulyk.exception.ValidationException;
import com.github.rrin.vulyk.repository.CommentRepository;
import com.github.rrin.vulyk.repository.PostRepository;
import com.github.rrin.vulyk.repository.UserRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentResponse create(Long postId, String principalEmail, CommentRequest request) {
        PostEntity post = postRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException("Post not found"));
        UserEntity author = requireUser(principalEmail);

        CommentEntity parent = null;
        if (request.getParentCommentId() != null) {
            parent = commentRepository.findById(request.getParentCommentId())
                .orElseThrow(() -> new NotFoundException("Parent comment not found"));
            if (!Objects.equals(parent.getPost().getId(), postId)) {
                throw new ValidationException("Parent comment must belong to the same post");
            }
        }

        CommentEntity comment = CommentEntity.builder()
            .content(request.getContent())
            .post(post)
            .author(author)
            .parentComment(parent)
            .build();

        commentRepository.save(comment);
        return toResponse(comment);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> listForPost(Long postId, Long parentCommentId, Pageable pageable) {
        if (parentCommentId != null) {
            CommentEntity parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new NotFoundException("Parent comment not found"));
            if (!Objects.equals(parent.getPost().getId(), postId)) {
                throw new ValidationException("Parent comment must belong to the same post");
            }
            return commentRepository.findAllByParentCommentId(parentCommentId, pageable)
                .map(this::toResponse);
        }

        return commentRepository.findAllByPostIdAndParentCommentIsNull(postId, pageable)
            .map(this::toResponse);
    }

    @Transactional
    public CommentResponse update(Long commentId, String principalEmail, CommentRequest request) {
        CommentEntity comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new NotFoundException("Comment not found"));
        requireOwnership(comment, principalEmail);
        comment.setContent(request.getContent());
        return toResponse(comment);
    }

    @Transactional
    public void delete(Long commentId, String principalEmail) {
        CommentEntity comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new NotFoundException("Comment not found"));
        requireOwnership(comment, principalEmail);
        commentRepository.delete(comment);
    }

    private void requireOwnership(CommentEntity comment, String principalEmail) {
        if (comment.getAuthor() == null || !comment.getAuthor().getEmail().equalsIgnoreCase(principalEmail)) {
            throw new ValidationException("Only the author can modify this comment");
        }
    }

    private UserEntity requireUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ValidationException("User not found"));
    }

    private CommentResponse toResponse(CommentEntity entity) {
        return new CommentResponse(
            entity.getId(),
            entity.getPost() != null ? entity.getPost().getId() : null,
            entity.getParentComment() != null ? entity.getParentComment().getId() : null,
            entity.getAuthor() != null ? entity.getAuthor().getUsername() : null,
            entity.getContent(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
