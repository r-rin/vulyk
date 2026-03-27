package com.github.rrin.vulyk.service.marketplace;

import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemStatus;
import java.math.BigDecimal;

public record MarketplaceBrowseCriteria(
    String query,
    MarketplaceItemStatus status,
    String category,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    boolean ownOnly,
    Long principalUserId,
    String sortBy,
    String sortDirection
) {
}