package com.github.rrin.vulyk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemEntity;
import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemStatus;
import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import com.github.rrin.vulyk.domain.entity.user.UserRole;
import com.github.rrin.vulyk.exception.NotFoundException;
import com.github.rrin.vulyk.repository.MarketplaceItemRepository;
import com.github.rrin.vulyk.repository.UserRepository;
import com.github.rrin.vulyk.service.MarketplaceService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class MarketplaceVisibilityTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MarketplaceService marketplaceService;

    @Autowired
    private MarketplaceItemRepository marketplaceItemRepository;

    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Test
    void publicBrowseCannotForceRemovedStatusAndCannotOpenRemovedItem() {
        UserEntity seller = userRepository.save(UserEntity.builder()
            .username("seller-" + UUID.randomUUID().toString().substring(0, 8))
            .email("seller-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test")
            .passwordHash("hash")
            .role(UserRole.USER)
            .build());

        marketplaceItemRepository.save(MarketplaceItemEntity.builder()
            .seller(seller)
            .title("Public Item")
            .description("Visible")
            .category("demo")
            .price(new BigDecimal("10.00"))
            .status(MarketplaceItemStatus.AVAILABLE)
            .build());

        MarketplaceItemEntity removed = marketplaceItemRepository.save(MarketplaceItemEntity.builder()
            .seller(seller)
            .title("Removed Secret")
            .description("Should stay hidden")
            .category("demo")
            .price(new BigDecimal("1.00"))
            .status(MarketplaceItemStatus.REMOVED)
            .build());

        Page<?> page = marketplaceService.list(
            PageRequest.of(0, 20),
            null,
            MarketplaceItemStatus.REMOVED,
            null,
            null,
            null,
            "createdAt",
            "desc",
            false,
            null
        );

        assertThat(page.getContent())
            .extracting("title")
            .doesNotContain("Removed Secret");

        assertThatThrownBy(() -> marketplaceService.get(removed.getId(), null))
            .isInstanceOf(NotFoundException.class);

        assertThat(marketplaceService.get(removed.getId(), seller.getEmail()).getTitle())
            .isEqualTo("Removed Secret");
    }
}