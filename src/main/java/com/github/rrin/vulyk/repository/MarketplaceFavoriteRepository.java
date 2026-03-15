package com.github.rrin.vulyk.repository;

import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceFavoriteEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketplaceFavoriteRepository extends JpaRepository<MarketplaceFavoriteEntity, Long> {

    Optional<MarketplaceFavoriteEntity> findByUserIdAndItemId(Long userId, Long itemId);

    boolean existsByUserIdAndItemId(Long userId, Long itemId);

    long countByItemId(Long itemId);

    Page<MarketplaceFavoriteEntity> findAllByUserId(Long userId, Pageable pageable);
}
