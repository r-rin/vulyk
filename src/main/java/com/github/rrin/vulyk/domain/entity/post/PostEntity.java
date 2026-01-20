package com.github.rrin.vulyk.domain.entity.post;

import com.github.rrin.vulyk.domain.Identifiable;
import com.github.rrin.vulyk.domain.entity.AuditableEntity;
import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "posts", indexes = {
    @Index(name = "idx_posts_author", columnList = "author_id"),
    @Index(name = "idx_posts_state", columnList = "state"),
    @Index(name = "idx_posts_created_at", columnList = "created_at")
})
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PostEntity extends AuditableEntity implements Identifiable<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    @Builder.Default
    private PostState state = PostState.DRAFT;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private UserEntity author;
}
