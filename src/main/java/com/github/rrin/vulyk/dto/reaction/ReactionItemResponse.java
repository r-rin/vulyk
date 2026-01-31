package com.github.rrin.vulyk.dto.reaction;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReactionItemResponse {
    private String targetType; // POST or COMMENT
    private Long targetId;
    private String title;
    private String snippet;
    private Instant likedAt;
}
