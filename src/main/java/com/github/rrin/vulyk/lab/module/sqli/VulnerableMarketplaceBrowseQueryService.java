package com.github.rrin.vulyk.lab.module.sqli;

import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemEntity;
import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemStatus;
import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.repository.MarketplaceItemRepository;
import com.github.rrin.vulyk.service.marketplace.MarketplaceBrowseCriteria;
import com.github.rrin.vulyk.service.marketplace.MarketplaceBrowseQueryService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Primary
@Service
@RequiredArgsConstructor
@ConditionalOnLabEnabled(SqlInjectionMarketplaceLab.LAB_ID)
public class VulnerableMarketplaceBrowseQueryService implements MarketplaceBrowseQueryService {

    private final MarketplaceItemRepository marketplaceItemRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<MarketplaceItemEntity> browse(MarketplaceBrowseCriteria criteria, Pageable pageable) {
        String whereClause = buildWhereClause(criteria);
        String orderClause = buildOrderClause(criteria);

        String idSql = "select mi.id from marketplace_items mi where mi.is_deleted = false "
            + whereClause
            + orderClause;
        String countSql = "select count(*) from marketplace_items mi where mi.is_deleted = false " + whereClause;

        @SuppressWarnings("unchecked")
        List<Number> idRows = entityManager.createNativeQuery(idSql)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();

        List<Long> ids = idRows.stream()
            .map(Number::longValue)
            .toList();

        List<MarketplaceItemEntity> content;
        if (ids.isEmpty()) {
            content = List.of();
        } else {
            Map<Long, MarketplaceItemEntity> itemsById = marketplaceItemRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(MarketplaceItemEntity::getId, Function.identity()));
            content = ids.stream()
                .map(itemsById::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        }

        Number total = (Number) entityManager.createNativeQuery(countSql).getSingleResult();
        return new PageImpl<>(content, pageable, total.longValue());
    }

    private String buildWhereClause(MarketplaceBrowseCriteria criteria) {
        StringBuilder sql = new StringBuilder();

        if (criteria.status() != null) {
            sql.append(" and mi.status = '")
                .append(criteria.status().name())
                .append("'");
        } else if (!criteria.ownOnly()) {
            sql.append(" and mi.status = '")
                .append(MarketplaceItemStatus.AVAILABLE.name())
                .append("'");
        }

        if (criteria.query() != null && !criteria.query().isBlank()) {
            String normalizedQuery = criteria.query().trim().toLowerCase();
            sql.append(" and (lower(mi.title) like '%")
                .append(normalizedQuery)
                .append("%' or lower(mi.description) like '%")
                .append(normalizedQuery)
                .append("%')");
        }

        if (criteria.category() != null) {
            sql.append(" and lower(mi.category) = '")
                .append(criteria.category().trim().toLowerCase())
                .append("'");
        }

        if (criteria.minPrice() != null) {
            sql.append(" and mi.price >= ")
                .append(criteria.minPrice());
        }
        if (criteria.maxPrice() != null) {
            sql.append(" and mi.price <= ")
                .append(criteria.maxPrice());
        }

        if (criteria.principalUserId() != null) {
            sql.append(" and mi.seller_id = ")
                .append(criteria.principalUserId());
        }

        return sql.toString();
    }

    private String buildOrderClause(MarketplaceBrowseCriteria criteria) {
        String property = switch ((criteria.sortBy() == null ? "" : criteria.sortBy().trim().toLowerCase())) {
            case "price" -> "mi.price";
            case "title" -> "mi.title";
            case "createdat", "created_at", "date", "created", "" -> "mi.created_at";
            default -> "mi.created_at";
        };

        String direction = "asc".equalsIgnoreCase(criteria.sortDirection()) ? "asc" : "desc";
        return " order by " + property + " " + direction;
    }
}