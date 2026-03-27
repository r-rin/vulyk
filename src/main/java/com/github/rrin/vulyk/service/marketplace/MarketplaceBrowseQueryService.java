package com.github.rrin.vulyk.service.marketplace;

import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MarketplaceBrowseQueryService {

    Page<MarketplaceItemEntity> browse(MarketplaceBrowseCriteria criteria, Pageable pageable);
}