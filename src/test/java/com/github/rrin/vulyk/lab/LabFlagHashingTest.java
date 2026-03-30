package com.github.rrin.vulyk.lab;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.rrin.vulyk.domain.entity.marketplace.MarketplaceItemEntity;
import com.github.rrin.vulyk.lab.entity.LabFlagEntity;
import com.github.rrin.vulyk.lab.entity.LabTaskProgressEntity;
import com.github.rrin.vulyk.lab.module.sqli.SqlInjectionMarketplaceLab;
import com.github.rrin.vulyk.lab.repository.LabFlagRepository;
import com.github.rrin.vulyk.lab.repository.LabTaskProgressRepository;
import com.github.rrin.vulyk.lab.service.LabProgressService;
import com.github.rrin.vulyk.repository.MarketplaceItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = "lab.enabled=SQLI-01")
class LabFlagHashingTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private LabProgressService labProgressService;

    @Autowired
    private LabFlagRepository labFlagRepository;

    @Autowired
    private LabTaskProgressRepository labTaskProgressRepository;

    @Autowired
    private MarketplaceItemRepository marketplaceItemRepository;

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Test
    void storesOnlyFlagHashesAndAcceptsRawSubmittedFlag() {
        MarketplaceItemEntity recoveryLedger = marketplaceItemRepository.findAll().stream()
            .filter(item -> "Admin Recovery Ledger".equals(item.getTitle()))
            .findFirst()
            .orElseThrow();

        String rawFlag = extractFlag(recoveryLedger.getDescription());

        LabFlagEntity storedFlag = labFlagRepository.findByLabIdAndTaskId(
            SqlInjectionMarketplaceLab.LAB_ID,
            SqlInjectionMarketplaceLab.TASK_ID
        ).orElseThrow();

        assertThat(storedFlag.getFlagHash())
            .startsWith("$2")
            .isNotEqualTo(rawFlag)
            .doesNotContain(rawFlag);

        labProgressService.submitFlag(SqlInjectionMarketplaceLab.LAB_ID, rawFlag);

        LabTaskProgressEntity progress = labTaskProgressRepository.findByLabIdAndTaskId(
            SqlInjectionMarketplaceLab.LAB_ID,
            SqlInjectionMarketplaceLab.TASK_ID
        ).orElseThrow();

        assertThat(progress.getEvidence())
            .isEqualTo("flag-submission:" + SqlInjectionMarketplaceLab.TASK_ID)
            .doesNotContain(rawFlag);
    }

    private String extractFlag(String description) {
        String marker = "Training flag: ";
        return description.substring(description.indexOf(marker) + marker.length()).trim();
    }
}