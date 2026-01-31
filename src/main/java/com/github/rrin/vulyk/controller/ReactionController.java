package com.github.rrin.vulyk.controller;

import com.github.rrin.vulyk.dto.reaction.ReactionStatusResponse;
import com.github.rrin.vulyk.dto.reaction.ReactionItemResponse;
import com.github.rrin.vulyk.service.ReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    @PostMapping("/posts/{postId}/likes")
    public ReactionStatusResponse likePost(
        @PathVariable Long postId,
        @AuthenticationPrincipal String principalEmail
    ) {
        return reactionService.likePost(postId, principalEmail);
    }

    @DeleteMapping("/posts/{postId}/likes")
    public ReactionStatusResponse unlikePost(
        @PathVariable Long postId,
        @AuthenticationPrincipal String principalEmail
    ) {
        return reactionService.unlikePost(postId, principalEmail);
    }

    @GetMapping("/posts/{postId}/likes")
    public ReactionStatusResponse postStatus(
        @PathVariable Long postId,
        @AuthenticationPrincipal String principalEmail
    ) {
        return reactionService.postStatus(postId, principalEmail);
    }

    @PostMapping("/comments/{commentId}/likes")
    public ReactionStatusResponse likeComment(
        @PathVariable Long commentId,
        @AuthenticationPrincipal String principalEmail
    ) {
        return reactionService.likeComment(commentId, principalEmail);
    }

    @DeleteMapping("/comments/{commentId}/likes")
    public ReactionStatusResponse unlikeComment(
        @PathVariable Long commentId,
        @AuthenticationPrincipal String principalEmail
    ) {
        return reactionService.unlikeComment(commentId, principalEmail);
    }

    @GetMapping("/comments/{commentId}/likes")
    public ReactionStatusResponse commentStatus(
        @PathVariable Long commentId,
        @AuthenticationPrincipal String principalEmail
    ) {
        return reactionService.commentStatus(commentId, principalEmail);
    }

    @GetMapping("/likes/posts")
    public Page<ReactionItemResponse> likedPosts(
        @AuthenticationPrincipal String principalEmail,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return reactionService.listLikedPosts(principalEmail, pageable);
    }

    @GetMapping("/likes/comments")
    public Page<ReactionItemResponse> likedComments(
        @AuthenticationPrincipal String principalEmail,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return reactionService.listLikedComments(principalEmail, pageable);
    }
}
