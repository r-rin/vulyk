package com.github.rrin.vulyk.service;

import com.github.rrin.vulyk.domain.entity.post.PostEntity;
import com.github.rrin.vulyk.domain.entity.post.PostState;
import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import com.github.rrin.vulyk.dto.post.PostRequest;
import com.github.rrin.vulyk.dto.post.PostResponse;
import com.github.rrin.vulyk.exception.NotFoundException;
import com.github.rrin.vulyk.exception.ValidationException;
import com.github.rrin.vulyk.repository.PostRepository;
import com.github.rrin.vulyk.repository.UserRepository;
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

    private static final Set<PostState> PUBLIC_VISIBLE_STATES = EnumSet.of(PostState.PUBLISHED);
    private static final Set<PostState> OWNER_VISIBLE_STATES = EnumSet.of(
        PostState.DRAFT,
        PostState.PUBLISHED,
        PostState.HIDDEN,
        PostState.REDACTED
    );
    private static final Set<PostState> CREATABLE_STATES = EnumSet.of(PostState.DRAFT, PostState.PUBLISHED);
    private static final Set<PostState> ALLOWED_STATE_TRANSITIONS = EnumSet.of(
        PostState.DRAFT,
        PostState.PUBLISHED,
        PostState.HIDDEN,
        PostState.REDACTED
    );

    @Transactional
    public PostResponse create(String principalEmail, PostRequest request) {
        UserEntity author = requireUser(principalEmail);
        PostState initialState = normalizeInitialState(request.getState());

        PostEntity post = PostEntity.builder()
            .title(request.getTitle())
            .content(request.getContent())
            .author(author)
            .state(initialState)
            .build();

        postRepository.save(post);
        return toResponse(post);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> listPublic(Pageable pageable, String query) {
        return listInternal(pageable, List.copyOf(PUBLIC_VISIBLE_STATES), query, null);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> listOwn(Pageable pageable, String principalEmail, List<PostState> states, String query) {
        UserEntity author = requireUser(principalEmail);
        return listInternal(pageable, normalizeOwnerStates(states), query, author.getId());
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> listByAuthorUsername(
        Pageable pageable,
        String username,
        List<PostState> states,
        String query
    ) {
        UserEntity author = userRepository.findByUsername(username)
            .orElseThrow(() -> new NotFoundException("User not found"));
        return listInternal(pageable, normalizeOwnerStates(states), query, author.getId());
    }

    @Transactional(readOnly = true)
    public PostResponse get(Long postId, String principalEmail) {
        return toResponse(requireViewablePostEntity(postId, principalEmail));
    }

    @Transactional(readOnly = true)
    public PostEntity requireViewablePostEntity(Long postId, String principalEmail) {
        PostEntity post = postRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException("Post not found"));

        if (!canView(post, principalEmail)) {
            throw new NotFoundException("Post not found");
        }

        return post;
    }

    private Page<PostResponse> listInternal(Pageable pageable, List<PostState> states, String query, Long authorId) {
        List<PostState> effectiveStates = (states == null || states.isEmpty())
            ? List.copyOf(PUBLIC_VISIBLE_STATES)
            : states;

        boolean hasQuery = query != null && !query.isBlank();

        if (authorId != null) {
            if (!hasQuery) {
                return postRepository.findAllByAuthorIdAndStateIn(authorId, effectiveStates, pageable)
                    .map(this::toResponse);
            }

            String term = query.trim();
            return postRepository.findAllByAuthorIdAndStateInAndTitleContainingIgnoreCaseOrAuthorIdAndStateInAndContentContainingIgnoreCase(
                authorId,
                effectiveStates,
                term,
                authorId,
                effectiveStates,
                term,
                pageable
            ).map(this::toResponse);
        }

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

    private boolean canView(PostEntity post, String principalEmail) {
        if (post.getState() != null && PUBLIC_VISIBLE_STATES.contains(post.getState())) {
            return true;
        }

        return isOwner(post, principalEmail)
            && post.getState() != null
            && OWNER_VISIBLE_STATES.contains(post.getState());
    }

    private boolean isOwner(PostEntity post, String principalEmail) {
        return principalEmail != null
            && !principalEmail.isBlank()
            && post.getAuthor() != null
            && post.getAuthor().getEmail() != null
            && post.getAuthor().getEmail().equalsIgnoreCase(principalEmail);
    }

    private List<PostState> normalizeOwnerStates(List<PostState> states) {
        if (states == null || states.isEmpty()) {
            return List.copyOf(PUBLIC_VISIBLE_STATES);
        }

        List<PostState> filtered = states.stream()
            .filter(OWNER_VISIBLE_STATES::contains)
            .toList();

        return filtered.isEmpty() ? List.copyOf(PUBLIC_VISIBLE_STATES) : filtered;
    }

    private PostState normalizeInitialState(PostState state) {
        if (state == null) {
            return PostState.PUBLISHED;
        }
        if (!CREATABLE_STATES.contains(state)) {
            throw new ValidationException("New posts can only be created as draft or published");
        }
        return state;
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
