package com.github.rrin.vulyk.repository;

import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import com.github.rrin.vulyk.domain.entity.user.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByPhoneNumber(String phoneNumber);

    Page<UserEntity> findAllByRole(UserRole role, Pageable pageable);

    @Query("SELECT u FROM UserEntity u WHERE u.email = ?1 OR u.username = ?1")
    Optional<UserEntity> findByEmailOrUsername(String identifier);
}
