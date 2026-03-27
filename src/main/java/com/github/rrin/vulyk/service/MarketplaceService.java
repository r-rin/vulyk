package com.github.rrin.vulyk.service;

import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemEntity;
import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceFavoriteEntity;
import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemStatus;
import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import com.github.rrin.vulyk.dto.marketplace.MarketplaceContactResponse;
import com.github.rrin.vulyk.dto.marketplace.MarketplaceFavoriteStatusResponse;
import com.github.rrin.vulyk.dto.marketplace.MarketplaceItemRequest;
import com.github.rrin.vulyk.dto.marketplace.MarketplaceItemResponse;
import com.github.rrin.vulyk.dto.marketplace.MarketplaceSellerResponse;
import com.github.rrin.vulyk.exception.NotFoundException;
import com.github.rrin.vulyk.exception.ValidationException;
import com.github.rrin.vulyk.repository.MarketplaceFavoriteRepository;
import com.github.rrin.vulyk.repository.MarketplaceItemRepository;
import com.github.rrin.vulyk.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.github.rrin.vulyk.service.marketplace.MarketplaceBrowseCriteria;
import com.github.rrin.vulyk.service.marketplace.MarketplaceBrowseQueryService;

@Service
@RequiredArgsConstructor
public class MarketplaceService {

    private final MarketplaceItemRepository marketplaceItemRepository;
    private final MarketplaceFavoriteRepository marketplaceFavoriteRepository;
    private final UserRepository userRepository;
    private final MarketplaceBrowseQueryService marketplaceBrowseQueryService;

    @Transactional
    public MarketplaceItemResponse create(String principalEmail, MarketplaceItemRequest request) {
        UserEntity seller = requireUser(principalEmail);

        MarketplaceItemEntity item = MarketplaceItemEntity.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .category(normalizeCategory(request.getCategory()))
            .price(request.getPrice())
            .status(request.getStatus() != null ? request.getStatus() : MarketplaceItemStatus.AVAILABLE)
            .seller(seller)
            .build();

        marketplaceItemRepository.save(item);
        return toResponse(item);
    }

    @Transactional(readOnly = true)
    public MarketplaceItemResponse get(Long itemId, String principalEmail) {
        MarketplaceItemEntity item = marketplaceItemRepository.findById(itemId)
            .orElseThrow(() -> new NotFoundException("Marketplace item not found"));

        if (!canViewItem(item, principalEmail)) {
            throw new NotFoundException("Marketplace item not found");
        }

        return toResponse(item);
    }

    @Transactional(readOnly = true)
    public Page<MarketplaceItemResponse> list(
        Pageable pageable,
        String query,
        MarketplaceItemStatus status,
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String sortBy,
        String sortDirection,
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

        Pageable effectivePageable = buildPageable(pageable, sortBy, sortDirection);
        String normalizedCategory = normalizeCategory(category);

        final Long principalUserId = Boolean.TRUE.equals(ownOnly)
            ? requireUser(principalEmail).getId()
            : null;

        MarketplaceItemStatus effectiveStatus = resolveVisibleStatus(status, Boolean.TRUE.equals(ownOnly));

        MarketplaceBrowseCriteria criteria = new MarketplaceBrowseCriteria(
            query,
            effectiveStatus,
            normalizedCategory,
            minPrice,
            maxPrice,
            Boolean.TRUE.equals(ownOnly),
            principalUserId,
            sortBy,
            sortDirection
        );

        return marketplaceBrowseQueryService.browse(criteria, effectivePageable)
            .map(this::toResponse);
    }

    @Transactional
    public MarketplaceItemResponse update(Long itemId, String principalEmail, MarketplaceItemRequest request) {
        MarketplaceItemEntity item = marketplaceItemRepository.findById(itemId)
            .orElseThrow(() -> new NotFoundException("Marketplace item not found"));
        requireOwnership(item, principalEmail);

        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setCategory(normalizeCategory(request.getCategory()));
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

    @Transactional(readOnly = true)
    public MarketplaceSellerResponse getSellerProfile(Long itemId) {
        MarketplaceItemEntity item = marketplaceItemRepository.findById(itemId)
            .orElseThrow(() -> new NotFoundException("Marketplace item not found"));

        UserEntity seller = item.getSeller();
        if (seller == null) {
            throw new NotFoundException("Seller not found");
        }

        return new MarketplaceSellerResponse(
            seller.getId(),
            seller.getUsername(),
            seller.getName(),
            seller.getBio()
        );
    }

    @Transactional(readOnly = true)
    public MarketplaceContactResponse contactSeller(Long itemId, String principalEmail) {
        UserEntity requester = requireUser(principalEmail);
        MarketplaceItemEntity item = marketplaceItemRepository.findById(itemId)
            .orElseThrow(() -> new NotFoundException("Marketplace item not found"));
        UserEntity seller = item.getSeller();

        if (seller == null) {
            throw new NotFoundException("Seller not found");
        }

        if (requester.getId().equals(seller.getId())) {
            throw new ValidationException("You cannot contact yourself as seller");
        }

        return new MarketplaceContactResponse(
            item.getId(),
            seller.getUsername(),
            seller.getEmail(),
            "Mock contact endpoint: use seller email to continue communication"
        );
    }

    @Transactional
    public MarketplaceFavoriteStatusResponse favorite(Long itemId, String principalEmail) {
        UserEntity user = requireUser(principalEmail);
        MarketplaceItemEntity item = marketplaceItemRepository.findById(itemId)
            .orElseThrow(() -> new NotFoundException("Marketplace item not found"));

        marketplaceFavoriteRepository.findByUserIdAndItemId(user.getId(), itemId)
            .orElseGet(() -> marketplaceFavoriteRepository.save(MarketplaceFavoriteEntity.builder()
                .user(user)
                .item(item)
                .build()));

        long count = marketplaceFavoriteRepository.countByItemId(itemId);
        return new MarketplaceFavoriteStatusResponse(true, count);
    }

    @Transactional
    public MarketplaceFavoriteStatusResponse unfavorite(Long itemId, String principalEmail) {
        UserEntity user = requireUser(principalEmail);
        marketplaceItemRepository.findById(itemId)
            .orElseThrow(() -> new NotFoundException("Marketplace item not found"));

        marketplaceFavoriteRepository.findByUserIdAndItemId(user.getId(), itemId)
            .ifPresent(marketplaceFavoriteRepository::delete);

        long count = marketplaceFavoriteRepository.countByItemId(itemId);
        return new MarketplaceFavoriteStatusResponse(false, count);
    }

    @Transactional(readOnly = true)
    public MarketplaceFavoriteStatusResponse favoriteStatus(Long itemId, String principalEmail) {
        UserEntity user = requireUser(principalEmail);
        marketplaceItemRepository.findById(itemId)
            .orElseThrow(() -> new NotFoundException("Marketplace item not found"));

        boolean favorite = marketplaceFavoriteRepository.existsByUserIdAndItemId(user.getId(), itemId);
        long count = marketplaceFavoriteRepository.countByItemId(itemId);
        return new MarketplaceFavoriteStatusResponse(favorite, count);
    }

    @Transactional(readOnly = true)
    public Page<MarketplaceItemResponse> listFavoriteItems(String principalEmail, Pageable pageable) {
        UserEntity user = requireUser(principalEmail);
        Page<MarketplaceFavoriteEntity> favorites = marketplaceFavoriteRepository.findAllByUserId(user.getId(), pageable);

        List<MarketplaceItemResponse> items = favorites.getContent().stream()
            .map(MarketplaceFavoriteEntity::getItem)
            .map(this::toResponse)
            .toList();

        return new PageImpl<>(items, pageable, favorites.getTotalElements());
    }

    private void requireOwnership(MarketplaceItemEntity item, String principalEmail) {
        if (item.getSeller() == null || !item.getSeller().getEmail().equalsIgnoreCase(principalEmail)) {
            throw new ValidationException("Only the seller can modify this marketplace item");
        }
    }

    private boolean canViewItem(MarketplaceItemEntity item, String principalEmail) {
        if (item.getStatus() == MarketplaceItemStatus.AVAILABLE) {
            return true;
        }

        if (principalEmail == null || principalEmail.isBlank()) {
            return false;
        }

        return item.getSeller() != null
            && item.getSeller().getEmail() != null
            && item.getSeller().getEmail().equalsIgnoreCase(principalEmail);
    }

    private MarketplaceItemStatus resolveVisibleStatus(MarketplaceItemStatus requestedStatus, boolean ownOnly) {
        if (!ownOnly) {
            return MarketplaceItemStatus.AVAILABLE;
        }
        return requestedStatus;
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
            entity.getCategory(),
            entity.getPrice(),
            entity.getStatus(),
            entity.getSeller() != null ? entity.getSeller().getUsername() : null,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private String normalizeCategory(String category) {
        if (category == null) {
            return null;
        }
        String normalized = category.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Pageable buildPageable(Pageable original, String sortBy, String sortDirection) {
        if (sortBy == null || sortBy.isBlank()) {
            return original;
        }

        String property = switch (sortBy.trim().toLowerCase()) {
            case "price" -> "price";
            case "createdat", "created_at", "date", "created" -> "createdAt";
            case "title" -> "title";
            default -> throw new ValidationException("Unsupported sortBy value: " + sortBy);
        };

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        return PageRequest.of(original.getPageNumber(), original.getPageSize(), Sort.by(direction, property));
    }
}
