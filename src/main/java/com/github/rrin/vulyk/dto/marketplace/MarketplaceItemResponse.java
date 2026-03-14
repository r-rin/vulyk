package com.github.rrin.vulyk.dto.marketplace;

import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemStatus;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceItemResponse {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private MarketplaceItemStatus status;
    private String sellerUsername;
    private Instant createdAt;
    private Instant updatedAt;
}
