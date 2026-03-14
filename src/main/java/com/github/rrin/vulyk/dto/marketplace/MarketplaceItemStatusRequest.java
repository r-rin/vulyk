package com.github.rrin.vulyk.dto.marketplace;

import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceItemStatusRequest {

    @NotNull
    private MarketplaceItemStatus status;
}
