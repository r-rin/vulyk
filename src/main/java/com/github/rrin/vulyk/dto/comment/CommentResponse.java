package com.github.rrin.vulyk.dto.comment;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private Long postId;
    private Long parentCommentId;
    private String authorUsername;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
}
