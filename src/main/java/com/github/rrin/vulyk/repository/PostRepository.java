package com.github.rrin.vulyk.repository;

import com.github.rrin.vulyk.domain.entity.post.PostEntity;
import com.github.rrin.vulyk.domain.entity.post.PostState;
import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, Long> {

    Page<PostEntity> findAllByState(PostState state, Pageable pageable);

    Page<PostEntity> findAllByStateIn(Collection<PostState> states, Pageable pageable);

    Page<PostEntity> findAllByAuthor(UserEntity author, Pageable pageable);

    Page<PostEntity> findAllByAuthorIdAndStateIn(Long authorId, Collection<PostState> states, Pageable pageable);

    Optional<PostEntity> findByIdAndAuthorId(Long id, Long authorId);

    boolean existsByIdAndAuthorId(Long id, Long authorId);
}
