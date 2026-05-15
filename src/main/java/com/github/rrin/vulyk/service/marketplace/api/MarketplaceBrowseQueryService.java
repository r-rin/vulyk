package com.github.rrin.vulyk.service.marketplace.api;

import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemEntity;
import com.github.rrin.vulyk.service.marketplace.MarketplaceBrowseCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MarketplaceBrowseQueryService {

    Page<MarketplaceItemEntity> browse(MarketplaceBrowseCriteria criteria, Pageable pageable);
}
