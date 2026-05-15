package com.github.rrin.vulyk.lab.module.sqli.sqli03;

import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemEntity;
import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemStatus;
import com.github.rrin.vulyk.domain.entity.user.UserEntity;
import com.github.rrin.vulyk.domain.entity.user.UserRole;
import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.lab.config.LabProperties;
import com.github.rrin.vulyk.lab.entity.LabFlagEntity;
import com.github.rrin.vulyk.lab.repository.LabFlagRepository;
import com.github.rrin.vulyk.lab.service.LabFlagVerificationService;
import com.github.rrin.vulyk.lab.service.LabProgressService;
import com.github.rrin.vulyk.repository.MarketplaceItemRepository;
import com.github.rrin.vulyk.repository.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnLabEnabled(SqlInjectionBooleanMarketplaceLab.LAB_ID)
public class SqlInjectionBooleanMarketplaceLabSeeder implements ApplicationRunner {

    public static final String TARGET_MARKER = "sqli03-title-oracle";
    public static final String TARGET_SELLER_USERNAME = "oracle-catalog";

    private final SqlInjectionBooleanMarketplaceLab labDefinition;
    private final LabProperties labProperties;
    private final LabFlagRepository labFlagRepository;
    private final LabFlagVerificationService labFlagVerificationService;
    private final LabProgressService labProgressService;
    private final UserRepository userRepository;
    private final MarketplaceItemRepository marketplaceItemRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        labProgressService.ensureProgressRows(labDefinition);

        UserEntity seller = ensureSeller(
            TARGET_SELLER_USERNAME,
            "oracle-catalog@vulyk.lab",
            "Oracle Catalog Keeper",
            "Maintains hidden marketplace incident records for training drills."
        );

        UserEntity trader = ensureSeller(
            "market-runner",
            "market-runner@vulyk.lab",
            "Market Runner",
            "Publishes normal public listings."
        );

        String visibleFlag = ensureFlagValue(seller);

        ensureItem(
            trader,
            "Copper Wire Pack",
            "Public stock with sealed packaging.",
            "electronics",
            new BigDecimal("14.99"),
            MarketplaceItemStatus.AVAILABLE
        );
        ensureItem(
            trader,
            "Field Notebook",
            "Weather-resistant pages and rigid cover.",
            "logistics",
            new BigDecimal("8.20"),
            MarketplaceItemStatus.AVAILABLE
        );
        ensureItem(
            seller,
            visibleFlag,
            TARGET_MARKER,
            "internal",
            new BigDecimal("0.01"),
            MarketplaceItemStatus.REMOVED
        );
    }

    private UserEntity ensureSeller(String username, String email, String name, String bio) {
        return userRepository.findByEmail(email)
            .orElseGet(() -> userRepository.save(UserEntity.builder()
                .username(username)
                .email(email)
                .name(name)
                .bio(bio)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(UserRole.USER)
                .build()));
    }

    private String ensureFlagValue(UserEntity seller) {
        return labFlagRepository.findByLabIdAndTaskId(labDefinition.getId(), SqlInjectionBooleanMarketplaceLab.TASK_ID)
            .map(existing -> upgradeAndResolveFlagValue(existing, seller))
            .orElseGet(() -> {
                String rawFlag = generateFlag();
                labFlagRepository.save(LabFlagEntity.builder()
                    .labId(labDefinition.getId())
                    .taskId(SqlInjectionBooleanMarketplaceLab.TASK_ID)
                    .flagHash(labFlagVerificationService.encode(rawFlag))
                    .seedContext("marketplace_items.title")
                    .build());
                return rawFlag;
            });
    }

    private String upgradeAndResolveFlagValue(LabFlagEntity existing, UserEntity seller) {
        String storedValue = existing.getFlagHash();
        if (storedValue != null && !storedValue.isBlank() && !storedValue.startsWith("$2")) {
            existing.setFlagHash(labFlagVerificationService.encode(storedValue));
            labFlagRepository.save(existing);
            return storedValue;
        }

        return marketplaceItemRepository.findAllBySellerId(seller.getId(), Pageable.unpaged())
            .stream()
            .filter(item -> TARGET_MARKER.equals(item.getDescription()))
            .map(MarketplaceItemEntity::getTitle)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Unable to recover seeded flag value for SQLI-03"));
    }

    private void ensureItem(
        UserEntity seller,
        String title,
        String description,
        String category,
        BigDecimal price,
        MarketplaceItemStatus status
    ) {
        MarketplaceItemEntity entity = marketplaceItemRepository.findBySellerIdAndTitleIgnoreCase(seller.getId(), title)
            .orElseGet(() -> MarketplaceItemEntity.builder()
                .seller(seller)
                .title(title)
                .build());

        entity.setDescription(description);
        entity.setCategory(category);
        entity.setPrice(price);
        entity.setStatus(status);
        marketplaceItemRepository.save(entity);
    }

    private String generateFlag() {
        return labProperties.getValidation().getFlagPrefix()
            + "sqli-03-"
            + UUID.randomUUID().toString().substring(0, 12)
            + labProperties.getValidation().getFlagSuffix();
    }
}
