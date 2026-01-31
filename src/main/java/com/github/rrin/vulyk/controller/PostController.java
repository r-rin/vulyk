package com.github.rrin.vulyk.controller;

import com.github.rrin.vulyk.domain.entity.post.PostState;
import com.github.rrin.vulyk.dto.post.PostRequest;
import com.github.rrin.vulyk.dto.post.PostResponse;
import com.github.rrin.vulyk.dto.post.PostStateRequest;
import com.github.rrin.vulyk.exception.ValidationException;
import com.github.rrin.vulyk.service.PostService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
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
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(
        @AuthenticationPrincipal String principalEmail,
        @Valid @RequestBody PostRequest request
    ) {
        return postService.create(principalEmail, request);
    }

    @GetMapping
    public Page<PostResponse> list(
        @PageableDefault(size = 20) Pageable pageable,
        @RequestParam(name = "state", required = false) List<String> stateFilters,
        @RequestParam(name = "q", required = false) String query
    ) {
        List<PostState> states = stateFilters == null ? null : stateFilters.stream()
            .map(s -> {
                try {
                    return PostState.valueOf(s.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    throw new ValidationException("Invalid post state: " + s);
                }
            })
            .toList();
        return postService.list(pageable, states, query);
    }

    @GetMapping("/{postId}")
    public PostResponse get(@PathVariable Long postId) {
        return postService.get(postId);
    }

    @PutMapping("/{postId}")
    public PostResponse update(
        @PathVariable Long postId,
        @AuthenticationPrincipal String principalEmail,
        @Valid @RequestBody PostRequest request
    ) {
        return postService.update(postId, principalEmail, request);
    }

    @PutMapping("/{postId}/state")
    public PostResponse updateState(
        @PathVariable Long postId,
        @AuthenticationPrincipal String principalEmail,
        @Valid @RequestBody PostStateRequest request
    ) {
        return postService.updateState(postId, principalEmail, request.getState());
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable Long postId,
        @AuthenticationPrincipal String principalEmail
    ) {
        postService.delete(postId, principalEmail);
    }
}
