package com.github.rrin.vulyk.domain.entity.post;

import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import com.github.rrin.vulyk.domain.enums.PostState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, insertable = false)
    private PostState state;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private UserEntity author;

    private Long createdAt;
    private Long updatedAt;
}
