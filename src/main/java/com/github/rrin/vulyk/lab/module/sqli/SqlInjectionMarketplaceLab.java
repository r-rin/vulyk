package com.github.rrin.vulyk.lab.module.sqli;

import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.lab.domain.LabDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskHintDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnLabEnabled(SqlInjectionMarketplaceLab.LAB_ID)
public class SqlInjectionMarketplaceLab implements LabDefinition {

    public static final String LAB_ID = "SQLI-01";
    public static final String TASK_ID = "extract-ledger-flag";

    @Override
    public String getId() {
        return LAB_ID;
    }

    @Override
    public String getTitle() {
        return "Marketplace Search Injection";
    }

    @Override
    public String getCategory() {
        return "SQL Injection";
    }

    @Override
    public String getDescription() {
        return "Exploit the public marketplace search to retrieve an internal removed record and recover the training flag.";
    }

    @Override
    public String getEntryPath() {
        return "/web/labs/" + LAB_ID;
    }

    @Override
    public List<LabTaskDefinition> getTasks() {
        return List.of(new LabTaskDefinition(
            TASK_ID,
            "Recover the admin ledger flag",
            "Use the public search box to surface the hidden recovery ledger entry and submit the flag it contains.",
            100,
            LabTaskMode.FLAG_SUBMISSION,
            List.of(
                new LabTaskHintDefinition(
                    "catalog-filter",
                    "Hint 1",
                    "The normal search only returns rows marked AVAILABLE. Focus on how the SQL predicate is assembled rather than on the visible results themselves.",
                    15
                ),
                new LabTaskHintDefinition(
                    "removed-ledger",
                    "Hint 2",
                    "The flag is embedded in the description of an item that was intentionally removed from the public catalog.",
                    25
                )
            )
        ));
    }
}