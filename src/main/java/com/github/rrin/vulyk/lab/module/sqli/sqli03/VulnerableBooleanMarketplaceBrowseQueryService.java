package com.github.rrin.vulyk.lab.module.sqli.sqli03;

import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemEntity;
import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemStatus;
import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.repository.MarketplaceItemRepository;
import com.github.rrin.vulyk.service.marketplace.MarketplaceBrowseCriteria;
import com.github.rrin.vulyk.service.marketplace.api.MarketplaceBrowseQueryService;
import com.github.rrin.vulyk.service.marketplace.impl.SecureMarketplaceBrowseQueryService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Locale;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Primary
@Service
@RequiredArgsConstructor
@ConditionalOnLabEnabled(SqlInjectionBooleanMarketplaceLab.LAB_ID)
public class VulnerableBooleanMarketplaceBrowseQueryService implements MarketplaceBrowseQueryService {

    private static final String ORACLE_SENTINEL = "sqli03-sentinel-";

    private final MarketplaceItemRepository marketplaceItemRepository;
    private final SecureMarketplaceBrowseQueryService secureMarketplaceBrowseQueryService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<MarketplaceItemEntity> browse(MarketplaceBrowseCriteria criteria, Pageable pageable) {
        String query = criteria.query() == null ? "" : criteria.query().trim();
        if (query.isBlank() || !looksLikeBooleanProbe(query)) {
            return secureMarketplaceBrowseQueryService.browse(criteria, pageable);
        }

        boolean oracleTrue = evaluatePredicate(query);
        if (!oracleTrue) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        return marketplaceItemRepository.findAllByStatus(MarketplaceItemStatus.AVAILABLE, pageable);
    }

    private boolean evaluatePredicate(String predicate) {
        String normalized = predicate == null ? "" : predicate.trim().toLowerCase();
        String sql = "select count(*) from marketplace_items mi"
            + " join users u on u.id = mi.seller_id"
            + " where mi.is_deleted = false"
            + " and u.is_deleted = false"
            + " and mi.status = '" + MarketplaceItemStatus.REMOVED.name() + "'"
            + " and lower(u.username) = '" + SqlInjectionBooleanMarketplaceLabSeeder.TARGET_SELLER_USERNAME + "'"
            + " and (lower(mi.title) like '%" + normalized + "%')";

        try {
            Number count = (Number) entityManager.createNativeQuery(sql).getSingleResult();
            return count.longValue() > 0;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean looksLikeBooleanProbe(String query) {
        String normalized = query.toLowerCase(Locale.ROOT);
        return normalized.contains("'")
            && (normalized.contains(" or ") || normalized.contains(" and ") || normalized.contains("substring("));
    }
}
