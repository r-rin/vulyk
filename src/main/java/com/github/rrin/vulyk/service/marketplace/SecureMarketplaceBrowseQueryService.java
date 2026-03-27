package com.github.rrin.vulyk.service.marketplace;

import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemEntity;
import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemStatus;
import com.github.rrin.vulyk.repository.MarketplaceItemRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecureMarketplaceBrowseQueryService implements MarketplaceBrowseQueryService {

    private final MarketplaceItemRepository marketplaceItemRepository;

    @Override
    public Page<MarketplaceItemEntity> browse(MarketplaceBrowseCriteria criteria, Pageable pageable) {
        Specification<MarketplaceItemEntity> specification = (root, queryObj, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.query() != null && !criteria.query().isBlank()) {
                String like = "%" + criteria.query().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("description")), like)
                ));
            }

            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            } else if (!criteria.ownOnly()) {
                predicates.add(cb.equal(root.get("status"), MarketplaceItemStatus.AVAILABLE));
            }

            if (criteria.category() != null) {
                predicates.add(cb.equal(cb.lower(root.get("category")), criteria.category().toLowerCase()));
            }

            if (criteria.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), criteria.minPrice()));
            }
            if (criteria.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), criteria.maxPrice()));
            }

            if (criteria.principalUserId() != null) {
                predicates.add(cb.equal(root.get("seller").get("id"), criteria.principalUserId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return marketplaceItemRepository.findAll(specification, pageable);
    }
}