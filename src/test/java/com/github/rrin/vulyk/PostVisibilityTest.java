package com.github.rrin.vulyk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.rrin.vulyk.domain.entity.post.PostEntity;
import com.github.rrin.vulyk.domain.entity.post.PostState;
import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import com.github.rrin.vulyk.domain.entity.user.UserRole;
import com.github.rrin.vulyk.dto.post.PostRequest;
import com.github.rrin.vulyk.exception.NotFoundException;
import com.github.rrin.vulyk.repository.PostRepository;
import com.github.rrin.vulyk.repository.UserRepository;
import com.github.rrin.vulyk.service.PostService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class PostVisibilityTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Test
    void publicFeedsHideNonPublishedPostsAndOwnerCanStillAccessDrafts() {
        UserEntity author = userRepository.save(UserEntity.builder()
            .username("author-" + UUID.randomUUID().toString().substring(0, 8))
            .email("author-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test")
            .passwordHash("hash")
            .role(UserRole.USER)
            .build());

        UserEntity viewer = userRepository.save(UserEntity.builder()
            .username("viewer-" + UUID.randomUUID().toString().substring(0, 8))
            .email("viewer-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test")
            .passwordHash("hash")
            .role(UserRole.USER)
            .build());

        PostEntity published = postRepository.save(PostEntity.builder()
            .title("Published Visible")
            .content("visible")
            .state(PostState.PUBLISHED)
            .author(author)
            .build());

        PostEntity draft = postRepository.save(PostEntity.builder()
            .title("Draft Hidden")
            .content("draft")
            .state(PostState.DRAFT)
            .author(author)
            .build());

        PostEntity redacted = postRepository.save(PostEntity.builder()
            .title("Redacted Hidden")
            .content("redacted")
            .state(PostState.REDACTED)
            .author(author)
            .build());

        assertThat(postService.listPublic(PageRequest.of(0, 20), null).getContent())
            .extracting("title")
            .contains("Published Visible")
            .doesNotContain("Draft Hidden", "Redacted Hidden");

        assertThatThrownBy(() -> postService.get(draft.getId(), viewer.getEmail()))
            .isInstanceOf(NotFoundException.class);

        assertThatThrownBy(() -> postService.get(redacted.getId(), null))
            .isInstanceOf(NotFoundException.class);

        assertThat(postService.get(draft.getId(), author.getEmail()).getTitle())
            .isEqualTo("Draft Hidden");

        assertThat(postService.create(author.getEmail(), new PostRequest("New Draft", "content", PostState.DRAFT)).getState())
            .isEqualTo(PostState.DRAFT);

        assertThat(postService.create(author.getEmail(), new PostRequest("New Published", "content", PostState.PUBLISHED)).getState())
            .isEqualTo(PostState.PUBLISHED);

        assertThat(postService.get(published.getId(), viewer.getEmail()).getTitle())
            .isEqualTo("Published Visible");
    }
}