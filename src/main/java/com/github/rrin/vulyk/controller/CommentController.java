package com.github.rrin.vulyk.controller;

import com.github.rrin.vulyk.dto.comment.CommentRequest;
import com.github.rrin.vulyk.dto.comment.CommentResponse;
import com.github.rrin.vulyk.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(
        @PathVariable Long postId,
        @AuthenticationPrincipal String principalEmail,
        @Valid @RequestBody CommentRequest request
    ) {
        return commentService.create(postId, principalEmail, request);
    }

    @GetMapping("/posts/{postId}/comments")
    public Page<CommentResponse> listForPost(
        @PathVariable Long postId,
        @RequestParam(value = "parentCommentId", required = false) Long parentCommentId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return commentService.listForPost(postId, parentCommentId, pageable);
    }

    @PutMapping("/comments/{commentId}")
    public CommentResponse update(
        @PathVariable Long commentId,
        @AuthenticationPrincipal String principalEmail,
        @Valid @RequestBody CommentRequest request
    ) {
        return commentService.update(commentId, principalEmail, request);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable Long commentId,
        @AuthenticationPrincipal String principalEmail
    ) {
        commentService.delete(commentId, principalEmail);
    }
}
