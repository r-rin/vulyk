package com.github.rrin.vulyk.service;

import com.github.rrin.vulyk.domain.entity.comment.CommentEntity;
import com.github.rrin.vulyk.domain.entity.post.PostEntity;
import com.github.rrin.vulyk.domain.entity.reaction.ReactionEntity;
import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import com.github.rrin.vulyk.dto.reaction.ReactionItemResponse;
import com.github.rrin.vulyk.dto.reaction.ReactionStatusResponse;
import com.github.rrin.vulyk.exception.NotFoundException;
import com.github.rrin.vulyk.exception.ValidationException;
import com.github.rrin.vulyk.repository.CommentRepository;
import com.github.rrin.vulyk.repository.PostRepository;
import com.github.rrin.vulyk.repository.ReactionRepository;
import com.github.rrin.vulyk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReactionStatusResponse likePost(Long postId, String principalEmail) {
        UserEntity user = requireUser(principalEmail);
        PostEntity post = postRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException("Post not found"));

        reactionRepository.findByUserIdAndPostId(user.getId(), postId)
            .orElseGet(() -> reactionRepository.save(ReactionEntity.builder()
                .user(user)
                .post(post)
                .build()));

        long count = reactionRepository.countByPostId(postId);
        return new ReactionStatusResponse(true, count);
    }

    @Transactional
    public ReactionStatusResponse unlikePost(Long postId, String principalEmail) {
        UserEntity user = requireUser(principalEmail);
        postRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException("Post not found"));

        reactionRepository.findByUserIdAndPostId(user.getId(), postId)
            .ifPresent(reactionRepository::delete);

        long count = reactionRepository.countByPostId(postId);
        return new ReactionStatusResponse(false, count);
    }

    @Transactional(readOnly = true)
    public ReactionStatusResponse postStatus(Long postId, String principalEmail) {
        UserEntity user = requireUser(principalEmail);
        postRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException("Post not found"));

        boolean liked = reactionRepository.existsByUserIdAndPostId(user.getId(), postId);
        long count = reactionRepository.countByPostId(postId);
        return new ReactionStatusResponse(liked, count);
    }

    @Transactional
    public ReactionStatusResponse likeComment(Long commentId, String principalEmail) {
        UserEntity user = requireUser(principalEmail);
        CommentEntity comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new NotFoundException("Comment not found"));

        reactionRepository.findByUserIdAndCommentId(user.getId(), commentId)
            .orElseGet(() -> reactionRepository.save(ReactionEntity.builder()
                .user(user)
                .comment(comment)
                .build()));

        long count = reactionRepository.countByCommentId(commentId);
        return new ReactionStatusResponse(true, count);
    }

    @Transactional
    public ReactionStatusResponse unlikeComment(Long commentId, String principalEmail) {
        UserEntity user = requireUser(principalEmail);
        commentRepository.findById(commentId)
            .orElseThrow(() -> new NotFoundException("Comment not found"));

        reactionRepository.findByUserIdAndCommentId(user.getId(), commentId)
            .ifPresent(reactionRepository::delete);

        long count = reactionRepository.countByCommentId(commentId);
        return new ReactionStatusResponse(false, count);
    }

    @Transactional(readOnly = true)
    public ReactionStatusResponse commentStatus(Long commentId, String principalEmail) {
        UserEntity user = requireUser(principalEmail);
        commentRepository.findById(commentId)
            .orElseThrow(() -> new NotFoundException("Comment not found"));

        boolean liked = reactionRepository.existsByUserIdAndCommentId(user.getId(), commentId);
        long count = reactionRepository.countByCommentId(commentId);
        return new ReactionStatusResponse(liked, count);
    }

    @Transactional(readOnly = true)
    public Page<ReactionItemResponse> listLikedPosts(String principalEmail, Pageable pageable) {
        UserEntity user = requireUser(principalEmail);
        return reactionRepository.findAllByUserIdAndPostIsNotNull(user.getId(), pageable)
            .map(this::toPostItem);
    }

    @Transactional(readOnly = true)
    public Page<ReactionItemResponse> listLikedComments(String principalEmail, Pageable pageable) {
        UserEntity user = requireUser(principalEmail);
        return reactionRepository.findAllByUserIdAndCommentIsNotNull(user.getId(), pageable)
            .map(this::toCommentItem);
    }

    private UserEntity requireUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ValidationException("User not found"));
    }

    private ReactionItemResponse toPostItem(ReactionEntity reaction) {
        PostEntity post = reaction.getPost();
        return new ReactionItemResponse(
            "POST",
            post != null ? post.getId() : null,
            post != null ? post.getTitle() : null,
            post != null ? shorten(post.getContent()) : null,
            reaction.getCreatedAt()
        );
    }

    private ReactionItemResponse toCommentItem(ReactionEntity reaction) {
        CommentEntity comment = reaction.getComment();
        return new ReactionItemResponse(
            "COMMENT",
            comment != null ? comment.getId() : null,
            null,
            comment != null ? shorten(comment.getContent()) : null,
            reaction.getCreatedAt()
        );
    }

    private String shorten(String text) {
        if (text == null) {
            return null;
        }
        if (text.length() <= 120) {
            return text;
        }
        return text.substring(0, 117) + "...";
    }
}
