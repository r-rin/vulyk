package com.github.rrin.vulyk.lab.module.sqli.sqli03;

import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.lab.domain.LabDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskHintDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnLabEnabled(SqlInjectionBooleanMarketplaceLab.LAB_ID)
public class SqlInjectionBooleanMarketplaceLab implements LabDefinition {

    public static final String LAB_ID = "SQLI-03";
    public static final String TASK_ID = "extract-hidden-title-flag";

    @Override
    public String getId() {
        return LAB_ID;
    }

    @Override
    public String getTitle() {
        return "Boolean Marketplace Oracle";
    }

    @Override
    public String getCategory() {
        return "SQL Injection";
    }

    @Override
    public String getDescription() {
        return "Use boolean-based blind SQL injection on marketplace search to infer a hidden removed item title and recover the training flag.";
    }

    @Override
    public String getEntryPath() {
        return "/web/labs/" + LAB_ID;
    }

    @Override
    public List<LabTaskDefinition> getTasks() {
        return List.of(new LabTaskDefinition(
            TASK_ID,
            "Extract the hidden title flag",
            "Use boolean predicates in the marketplace search query to infer the hidden removed item title and submit the flag.",
            100,
            LabTaskMode.FLAG_SUBMISSION,
            List.of(
                new LabTaskHintDefinition(
                    "hidden-title",
                    "Hint 1",
                    "The secret is in a hidden item's title. Escape out of the quoted search pattern, then inject a boolean probe like substring(lower(mi.title), 1, 1) = 'f'.",
                    15
                ),
                new LabTaskHintDefinition(
                    "flag-shape",
                    "Hint 2",
                    "The extracted value uses the standard training format: flag{...}.",
                    20
                )
            )
        ));
    }
}
