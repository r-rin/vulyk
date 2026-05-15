package com.github.rrin.vulyk.lab.module.sqli.sqli01;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnLabEnabled(SqlInjectionMarketplaceLab.LAB_ID)
public class SqlInjectionMarketplaceLabSeeder implements ApplicationRunner {

    private final SqlInjectionMarketplaceLab labDefinition;
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
            "lab-archivist",
            "archivist@vulyk.lab",
            "Archive Custodian",
            "Maintains item restoration records and internal recovery notes."
        );

        UserEntity trader = ensureSeller(
            "field-trader",
            "trader@vulyk.lab",
            "Field Trader",
            "Runs the public offers board for equipment and surplus parts."
        );

        String visibleFlag = ensureFlagValue(seller);

        ensureItem(
            seller,
            "Workbench Lamp",
            "Stable task lighting with a grounded steel arm and spare bulb included.",
            "equipment",
            new BigDecimal("49.90"),
            MarketplaceItemStatus.AVAILABLE
        );
        ensureItem(
            trader,
            "Courier Satchel",
            "Canvas dispatch satchel with reinforced stitching and brass buckle.",
            "logistics",
            new BigDecimal("24.50"),
            MarketplaceItemStatus.AVAILABLE
        );
        ensureItem(
            trader,
            "Retro Terminal Keyboard",
            "Mechanical terminal keyboard sourced from a decommissioned operations desk.",
            "electronics",
            new BigDecimal("89.00"),
            MarketplaceItemStatus.AVAILABLE
        );
        ensureItem(
            seller,
            "Admin Recovery Ledger",
            "Internal recovery notes. Training flag: " + visibleFlag,
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
        return labFlagRepository.findByLabIdAndTaskId(labDefinition.getId(), SqlInjectionMarketplaceLab.TASK_ID)
            .map(existing -> upgradeAndResolveFlagValue(existing, seller))
            .orElseGet(() -> {
                String rawFlag = generateFlag();
                labFlagRepository.save(LabFlagEntity.builder()
                    .labId(labDefinition.getId())
                    .taskId(SqlInjectionMarketplaceLab.TASK_ID)
                    .flagHash(labFlagVerificationService.encode(rawFlag))
                    .seedContext("marketplace_items.description")
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

        return marketplaceItemRepository.findBySellerIdAndTitleIgnoreCase(seller.getId(), "Admin Recovery Ledger")
            .map(MarketplaceItemEntity::getDescription)
            .map(this::extractFlagValue)
            .orElseThrow(() -> new IllegalStateException("Unable to recover seeded flag value for SQLI-01"));
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
            + "sqli-marketplace-"
            + UUID.randomUUID().toString().substring(0, 12)
            + labProperties.getValidation().getFlagSuffix();
    }

    private String extractFlagValue(String description) {
        String marker = "Training flag: ";
        int markerIndex = description == null ? -1 : description.indexOf(marker);
        if (markerIndex < 0) {
            throw new IllegalStateException("Seeded marketplace item no longer contains a recoverable flag value");
        }

        return description.substring(markerIndex + marker.length()).trim();
    }
}
