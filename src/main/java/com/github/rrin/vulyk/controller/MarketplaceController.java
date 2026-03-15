package com.github.rrin.vulyk.controller;

import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemStatus;
import com.github.rrin.vulyk.dto.marketplace.MarketplaceContactResponse;
import com.github.rrin.vulyk.dto.marketplace.MarketplaceFavoriteStatusResponse;
import com.github.rrin.vulyk.dto.marketplace.MarketplaceItemRequest;
import com.github.rrin.vulyk.dto.marketplace.MarketplaceItemResponse;
import com.github.rrin.vulyk.dto.marketplace.MarketplaceSellerResponse;
import com.github.rrin.vulyk.dto.marketplace.MarketplaceItemStatusRequest;
import com.github.rrin.vulyk.exception.ValidationException;
import com.github.rrin.vulyk.service.MarketplaceService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/marketplace/items")
@RequiredArgsConstructor
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MarketplaceItemResponse create(
        @AuthenticationPrincipal String principalEmail,
        @Valid @RequestBody MarketplaceItemRequest request
    ) {
        return marketplaceService.create(principalEmail, request);
    }

    @GetMapping
    public Page<MarketplaceItemResponse> list(
        @PageableDefault(size = 20) Pageable pageable,
        @RequestParam(name = "q", required = false) String query,
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "category", required = false) String category,
        @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
        @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
        @RequestParam(name = "sortBy", required = false) String sortBy,
        @RequestParam(name = "sortDir", required = false, defaultValue = "desc") String sortDir,
        @RequestParam(name = "ownOnly", required = false, defaultValue = "false") Boolean ownOnly,
        @AuthenticationPrincipal String principalEmail
    ) {
        MarketplaceItemStatus parsedStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                parsedStatus = MarketplaceItemStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ValidationException("Invalid marketplace status: " + status);
            }
        }

        return marketplaceService.list(
            pageable,
            query,
            parsedStatus,
            category,
            minPrice,
            maxPrice,
            sortBy,
            sortDir,
            ownOnly,
            principalEmail
        );
    }

    @GetMapping("/{itemId}")
    public MarketplaceItemResponse get(@PathVariable Long itemId) {
        return marketplaceService.get(itemId);
    }

    @PutMapping("/{itemId}")
    public MarketplaceItemResponse update(
        @PathVariable Long itemId,
        @AuthenticationPrincipal String principalEmail,
        @Valid @RequestBody MarketplaceItemRequest request
    ) {
        return marketplaceService.update(itemId, principalEmail, request);
    }

    @PutMapping("/{itemId}/status")
    public MarketplaceItemResponse updateStatus(
        @PathVariable Long itemId,
        @AuthenticationPrincipal String principalEmail,
        @Valid @RequestBody MarketplaceItemStatusRequest request
    ) {
        return marketplaceService.updateStatus(itemId, principalEmail, request.getStatus());
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable Long itemId,
        @AuthenticationPrincipal String principalEmail
    ) {
        marketplaceService.delete(itemId, principalEmail);
    }

    @GetMapping("/{itemId}/seller")
    public MarketplaceSellerResponse sellerProfile(@PathVariable Long itemId) {
        return marketplaceService.getSellerProfile(itemId);
    }

    @GetMapping("/{itemId}/contact")
    public MarketplaceContactResponse contactSeller(
        @PathVariable Long itemId,
        @AuthenticationPrincipal String principalEmail
    ) {
        return marketplaceService.contactSeller(itemId, principalEmail);
    }

    @PostMapping("/{itemId}/favorite")
    public MarketplaceFavoriteStatusResponse favorite(
        @PathVariable Long itemId,
        @AuthenticationPrincipal String principalEmail
    ) {
        return marketplaceService.favorite(itemId, principalEmail);
    }

    @DeleteMapping("/{itemId}/favorite")
    public MarketplaceFavoriteStatusResponse unfavorite(
        @PathVariable Long itemId,
        @AuthenticationPrincipal String principalEmail
    ) {
        return marketplaceService.unfavorite(itemId, principalEmail);
    }

    @GetMapping("/{itemId}/favorite")
    public MarketplaceFavoriteStatusResponse favoriteStatus(
        @PathVariable Long itemId,
        @AuthenticationPrincipal String principalEmail
    ) {
        return marketplaceService.favoriteStatus(itemId, principalEmail);
    }

    @GetMapping("/favorites")
    public Page<MarketplaceItemResponse> favoriteItems(
        @AuthenticationPrincipal String principalEmail,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return marketplaceService.listFavoriteItems(principalEmail, pageable);
    }
}
