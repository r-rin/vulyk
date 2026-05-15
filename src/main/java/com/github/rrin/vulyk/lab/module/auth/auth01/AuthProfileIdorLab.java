package com.github.rrin.vulyk.lab.module.auth.auth01;

import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.lab.domain.LabDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskHintDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnLabEnabled(AuthProfileIdorLab.LAB_ID)
public class AuthProfileIdorLab implements LabDefinition {

    public static final String LAB_ID = "AUTH-01";
    public static final String TASK_ID = "modify-foreign-profile";

    @Override
    public String getId() {
        return LAB_ID;
    }

    @Override
    public String getTitle() {
        return "Profile IDOR";
    }

    @Override
    public String getCategory() {
        return "Broken Access Control";
    }

    @Override
    public String getDescription() {
        return "Exploit an insecure profile update endpoint that trusts a user identifier in the request path and allows cross-user writes.";
    }

    @Override
    public String getEntryPath() {
        return "/web/labs/" + LAB_ID;
    }

    @Override
    public List<LabTaskDefinition> getTasks() {
        return List.of(new LabTaskDefinition(
            TASK_ID,
            "Modify another user's profile",
            "Use the vulnerable profile endpoint to update another account and trigger state-tracked completion.",
            100,
            LabTaskMode.STATE_TRACKED,
            List.of(
                new LabTaskHintDefinition(
                    "idor-endpoint",
                    "Hint 1",
                    "The vulnerable route accepts a profile identifier in path form: /users/profile/{username}.",
                    10
                ),
                new LabTaskHintDefinition(
                    "target-user",
                    "Hint 2",
                    "A seeded target account is named ops-manager. Change one visible field such as bio.",
                    20
                )
            )
        ));
    }
}
