package com.github.rrin.vulyk.repository;

import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemEntity;
import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemStatus;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketplaceItemRepository extends JpaRepository<MarketplaceItemEntity, Long>,
    JpaSpecificationExecutor<MarketplaceItemEntity> {

    Page<MarketplaceItemEntity> findAllBySellerId(Long sellerId, Pageable pageable);

    Page<MarketplaceItemEntity> findAllByStatus(MarketplaceItemStatus status, Pageable pageable);

    Page<MarketplaceItemEntity> findAllByStatusAndPriceBetween(
        MarketplaceItemStatus status,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Pageable pageable
    );

    Page<MarketplaceItemEntity> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
        String titleQuery,
        String descriptionQuery,
        Pageable pageable
    );

    Optional<MarketplaceItemEntity> findByIdAndSellerId(Long id, Long sellerId);

    Optional<MarketplaceItemEntity> findBySellerIdAndTitleIgnoreCase(Long sellerId, String title);
}
