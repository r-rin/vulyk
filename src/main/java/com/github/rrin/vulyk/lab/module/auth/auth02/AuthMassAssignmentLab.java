package com.github.rrin.vulyk.lab.module.auth.auth02;

import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.lab.domain.LabDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskHintDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnLabEnabled(AuthMassAssignmentLab.LAB_ID)
public class AuthMassAssignmentLab implements LabDefinition {

    public static final String LAB_ID = "AUTH-02";
    public static final String TASK_INJECT_ID = "inject-privileged-fields";

    @Override
    public String getId() {
        return LAB_ID;
    }

    @Override
    public String getTitle() {
        return "Mass Assignment Role Escalation";
    }

    @Override
    public String getCategory() {
        return "Broken Access Control";
    }

    @Override
    public String getDescription() {
        return "Exploit over-posting in web profile updates to inject privileged fields and escalate account role.";
    }

    @Override
    public String getEntryPath() {
        return "/web/labs/" + LAB_ID;
    }

    @Override
    public List<LabTaskDefinition> getTasks() {
        return List.of(
            new LabTaskDefinition(
                TASK_INJECT_ID,
                "Inject privileged fields",
                "Send an over-posted payload to POST /web/profile/edit that changes your role to ADMIN.",
                100,
                LabTaskMode.STATE_TRACKED,
                List.of(
                    new LabTaskHintDefinition(
                        "payload-shape",
                        "Hint 1",
                        "Submit standard profile form fields plus an extra role=ADMIN field.",
                        10
                    )
                )
            )
        );
    }
}
