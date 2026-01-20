package com.github.rrin.vulyk.domain.entity.reaction;

import com.github.rrin.vulyk.domain.Identifiable;
import com.github.rrin.vulyk.domain.entity.AuditableEntity;
import com.github.rrin.vulyk.domain.entity.comment.CommentEntity;
import com.github.rrin.vulyk.domain.entity.post.PostEntity;
import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "likes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "post_id"}),
    @UniqueConstraint(columnNames = {"user_id", "comment_id"})
}, indexes = {
    @Index(name = "idx_likes_user", columnList = "user_id"),
    @Index(name = "idx_likes_post", columnList = "post_id"),
    @Index(name = "idx_likes_comment", columnList = "comment_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionEntity extends AuditableEntity implements Identifiable<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private CommentEntity comment;
}

