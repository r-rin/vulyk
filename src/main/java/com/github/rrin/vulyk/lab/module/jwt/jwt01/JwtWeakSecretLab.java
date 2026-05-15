package com.github.rrin.vulyk.lab.module.jwt.jwt01;

import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.lab.domain.LabDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskHintDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnLabEnabled(JwtWeakSecretLab.LAB_ID)
public class JwtWeakSecretLab implements LabDefinition {

    public static final String LAB_ID = "JWT-01";
    public static final String TASK_FORGE_ADMIN_TOKEN = "forge-admin-token";

    @Override
    public String getId() {
        return LAB_ID;
    }

    @Override
    public String getTitle() {
        return "Weak JWT Secret Forgery";
    }

    @Override
    public String getCategory() {
        return "Authentication";
    }

    @Override
    public String getDescription() {
        return "Forge an admin JWT signed with a predictable secret and use it against core authenticated endpoints.";
    }

    @Override
    public String getEntryPath() {
        return "/web/labs/" + LAB_ID;
    }

    @Override
    public List<LabTaskDefinition> getTasks() {
        return List.of(
            new LabTaskDefinition(
                TASK_FORGE_ADMIN_TOKEN,
                "Forge an admin token",
                "Generate a valid JWT with role=ADMIN and use it on a core authenticated route like GET /users/me.",
                100,
                LabTaskMode.STATE_TRACKED,
                List.of(
                    new LabTaskHintDefinition(
                        "token-shape",
                        "Hint 1",
                        "The lab uses a weak shared secret and trusts the role claim from the token.",
                        10
                    ),
                    new LabTaskHintDefinition(
                        "weak-secret",
                        "Hint 2",
                        "Try `qwerty` as the signing key.",
                        20
                    )
                )
            )
        );
    }
}
