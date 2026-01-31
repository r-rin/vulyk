package com.github.rrin.vulyk.service;

import com.github.rrin.vulyk.domain.entity.post.PostEntity;
import com.github.rrin.vulyk.domain.entity.post.PostState;
import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import com.github.rrin.vulyk.exception.NotFoundException;
import com.github.rrin.vulyk.exception.ValidationException;
import com.github.rrin.vulyk.repository.PostRepository;
import com.github.rrin.vulyk.repository.UserRepository;
import com.github.rrin.vulyk.dto.post.PostRequest;
import com.github.rrin.vulyk.dto.post.PostResponse;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    private static final Set<PostState> DEFAULT_VISIBLE_STATES = EnumSet.of(PostState.PUBLISHED);
    private static final Set<PostState> ALLOWED_STATE_TRANSITIONS = EnumSet.of(
        PostState.DRAFT,
        PostState.PUBLISHED,
        PostState.HIDDEN,
        PostState.REDACTED
    );

    @Transactional
    public PostResponse create(String principalEmail, PostRequest request) {
        UserEntity author = requireUser(principalEmail);

        PostEntity post = PostEntity.builder()
            .title(request.getTitle())
            .content(request.getContent())
            .author(author)
            .state(PostState.PUBLISHED)
            .build();

        postRepository.save(post);
        return toResponse(post);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> list(Pageable pageable, List<PostState> states, String query) {
        List<PostState> effectiveStates = (states == null || states.isEmpty())
            ? List.copyOf(DEFAULT_VISIBLE_STATES)
            : states;

        boolean hasQuery = query != null && !query.isBlank();

        if (!hasQuery) {
            return postRepository.findAllByStateIn(effectiveStates, pageable).map(this::toResponse);
        }

        String term = query.trim();
        return postRepository
            .findByStateInAndTitleContainingIgnoreCaseOrStateInAndContentContainingIgnoreCase(
                effectiveStates,
                term,
                effectiveStates,
                term,
                pageable
            )
            .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PostResponse get(Long postId) {
        PostEntity post = postRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException("Post not found"));
        return toResponse(post);
    }

    @Transactional
    public PostResponse update(Long postId, String principalEmail, PostRequest request) {
        PostEntity post = postRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException("Post not found"));
        requireOwnership(post, principalEmail);

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        return toResponse(post);
    }

    @Transactional
    public PostResponse updateState(Long postId, String principalEmail, PostState state) {
        PostEntity post = postRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException("Post not found"));
        requireOwnership(post, principalEmail);
        if (state == null || !ALLOWED_STATE_TRANSITIONS.contains(state)) {
            throw new ValidationException("Unsupported post state transition");
        }
        post.setState(state);
        return toResponse(post);
    }

    @Transactional
    public void delete(Long postId, String principalEmail) {
        PostEntity post = postRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException("Post not found"));
        requireOwnership(post, principalEmail);
        postRepository.delete(post);
    }

    private void requireOwnership(PostEntity post, String principalEmail) {
        if (post.getAuthor() == null || !post.getAuthor().getEmail().equalsIgnoreCase(principalEmail)) {
            throw new ValidationException("Only the author can modify this post");
        }
    }

    private UserEntity requireUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ValidationException("User not found"));
    }

    private PostResponse toResponse(PostEntity post) {
        return new PostResponse(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            post.getState(),
            post.getAuthor() != null ? post.getAuthor().getUsername() : null,
            post.getCreatedAt(),
            post.getUpdatedAt()
        );
    }
}
