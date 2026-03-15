package com.github.rrin.vulyk.dto.marketplace;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceContactResponse {
    private Long itemId;
    private String sellerUsername;
    private String sellerEmail;
    private String message;
}
