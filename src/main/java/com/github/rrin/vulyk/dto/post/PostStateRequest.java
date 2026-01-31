package com.github.rrin.vulyk.dto.post;

import com.github.rrin.vulyk.domain.entity.post.PostState;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostStateRequest {

    @NotNull
    private PostState state;
}
