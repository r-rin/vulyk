package com.github.rrin.vulyk.dto.post;

import com.github.rrin.vulyk.domain.entity.post.PostState;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private PostState state;
    private String authorUsername;
    private Instant createdAt;
    private Instant updatedAt;
}
