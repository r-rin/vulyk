package com.github.rrin.vulyk.service;

import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemEntity;
import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemStatus;
import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import com.github.rrin.vulyk.dto.marketplace.MarketplaceItemRequest;
import com.github.rrin.vulyk.dto.marketplace.MarketplaceItemResponse;
import com.github.rrin.vulyk.exception.NotFoundException;
import com.github.rrin.vulyk.exception.ValidationException;
import com.github.rrin.vulyk.repository.MarketplaceItemRepository;
import com.github.rrin.vulyk.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarketplaceService {

    private final MarketplaceItemRepository marketplaceItemRepository;
    private final UserRepository userRepository;

    @Transactional
    public MarketplaceItemResponse create(String principalEmail, MarketplaceItemRequest request) {
        UserEntity seller = requireUser(principalEmail);

        MarketplaceItemEntity item = MarketplaceItemEntity.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .price(request.getPrice())
            .status(request.getStatus() != null ? request.getStatus() : MarketplaceItemStatus.AVAILABLE)
            .seller(seller)
            .build();

        marketplaceItemRepository.save(item);
        return toResponse(item);
    }

    @Transactional(readOnly = true)
    public MarketplaceItemResponse get(Long itemId) {
        MarketplaceItemEntity item = marketplaceItemRepository.findById(itemId)
            .orElseThrow(() -> new NotFoundException("Marketplace item not found"));
        return toResponse(item);
    }

    @Transactional(readOnly = true)
    public Page<MarketplaceItemResponse> list(
        Pageable pageable,
        String query,
        MarketplaceItemStatus status,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean ownOnly,
        String principalEmail
    ) {
        if (minPrice != null && minPrice.signum() < 0) {
            throw new ValidationException("minPrice must be non-negative");
        }
        if (maxPrice != null && maxPrice.signum() < 0) {
            throw new ValidationException("maxPrice must be non-negative");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new ValidationException("minPrice must be less than or equal to maxPrice");
        }

        Long principalUserId = null;
        if (Boolean.TRUE.equals(ownOnly)) {
            principalUserId = requireUser(principalEmail).getId();
        }

        Specification<MarketplaceItemEntity> specification = (root, queryObj, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("description")), like)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else if (!Boolean.TRUE.equals(ownOnly)) {
                predicates.add(cb.equal(root.get("status"), MarketplaceItemStatus.AVAILABLE));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (principalUserId != null) {
                predicates.add(cb.equal(root.get("seller").get("id"), principalUserId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return marketplaceItemRepository.findAll(specification, pageable)
            .map(this::toResponse);
    }

    @Transactional
    public MarketplaceItemResponse update(Long itemId, String principalEmail, MarketplaceItemRequest request) {
        MarketplaceItemEntity item = marketplaceItemRepository.findById(itemId)
            .orElseThrow(() -> new NotFoundException("Marketplace item not found"));
        requireOwnership(item, principalEmail);

        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        if (request.getStatus() != null) {
            item.setStatus(request.getStatus());
        }

        return toResponse(item);
    }

    @Transactional
    public MarketplaceItemResponse updateStatus(Long itemId, String principalEmail, MarketplaceItemStatus status) {
        MarketplaceItemEntity item = marketplaceItemRepository.findById(itemId)
            .orElseThrow(() -> new NotFoundException("Marketplace item not found"));
        requireOwnership(item, principalEmail);

        if (status == null) {
            throw new ValidationException("Marketplace item status is required");
        }

        item.setStatus(status);
        return toResponse(item);
    }

    @Transactional
    public void delete(Long itemId, String principalEmail) {
        MarketplaceItemEntity item = marketplaceItemRepository.findById(itemId)
            .orElseThrow(() -> new NotFoundException("Marketplace item not found"));
        requireOwnership(item, principalEmail);
        marketplaceItemRepository.delete(item);
    }

    private void requireOwnership(MarketplaceItemEntity item, String principalEmail) {
        if (item.getSeller() == null || !item.getSeller().getEmail().equalsIgnoreCase(principalEmail)) {
            throw new ValidationException("Only the seller can modify this marketplace item");
        }
    }

    private UserEntity requireUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ValidationException("User not found"));
    }

    private MarketplaceItemResponse toResponse(MarketplaceItemEntity entity) {
        return new MarketplaceItemResponse(
            entity.getId(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getPrice(),
            entity.getStatus(),
            entity.getSeller() != null ? entity.getSeller().getUsername() : null,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
