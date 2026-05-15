package com.github.rrin.vulyk.lab.module.sqli.sqli02;

import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.lab.domain.LabDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskHintDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnLabEnabled(SqlInjectionLoginLab.LAB_ID)
public class SqlInjectionLoginLab implements LabDefinition {

    public static final String LAB_ID = "SQLI-02";
    public static final String TASK_DISCOVER_ID = "discover-login-oracle";
    public static final String TASK_RECOVER_ID = "recover-admin-secret";

    @Override
    public String getId() {
        return LAB_ID;
    }

    @Override
    public String getTitle() {
        return "Login SQL Injection";
    }

    @Override
    public String getCategory() {
        return "SQL Injection";
    }

    @Override
    public String getDescription() {
        return "Exploit SQL injection in the login identifier field to impersonate another account and recover a flag hidden in that account's draft posts.";
    }

    @Override
    public String getEntryPath() {
        return "/web/labs/" + LAB_ID;
    }

    @Override
    public List<LabTaskDefinition> getTasks() {
        return List.of(
            new LabTaskDefinition(
                TASK_DISCOVER_ID,
                "Trigger identifier-based login bypass",
                "Use SQL injection in the identifier field to force a successful login as another account.",
                40,
                LabTaskMode.STATE_TRACKED,
                List.of(
                    new LabTaskHintDefinition(
                        "injection-target",
                        "Hint 1",
                        "Inject through identifier, not password. Use quote termination plus a boolean OR branch.",
                        10
                    ),
                    new LabTaskHintDefinition(
                        "operator-precedence",
                        "Hint 2",
                        "Try shaping the OR branch to resolve directly to a known username from the public post feed.",
                        15
                    )
                )
            ),
            new LabTaskDefinition(
                TASK_RECOVER_ID,
                "Recover and submit draft flag",
                "Log in as the seeded target account through identifier injection, open draft posts, and submit the flag.",
                60,
                LabTaskMode.FLAG_SUBMISSION,
                List.of(
                    new LabTaskHintDefinition(
                        "admin-identity",
                        "Hint 1",
                        "At least one published post from the target account is visible in the feed to aid discovery.",
                        10
                    ),
                    new LabTaskHintDefinition(
                        "flag-location",
                        "Hint 2",
                        "After takeover, open /web/posts?tab=drafts and look for content containing 'Training flag: '.",
                        20
                    )
                )
            )
        );
    }
}
