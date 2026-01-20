package com.github.rrin.vulyk.domain.entity.comment;

import com.github.rrin.vulyk.domain.Identifiable;
import com.github.rrin.vulyk.domain.entity.AuditableEntity;
import com.github.rrin.vulyk.domain.entity.post.PostEntity;
import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_comments_post", columnList = "post_id"),
    @Index(name = "idx_comments_author", columnList = "author_id"),
    @Index(name = "idx_comments_parent", columnList = "parent_comment_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CommentEntity extends AuditableEntity implements Identifiable<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private CommentEntity parentComment;
}
