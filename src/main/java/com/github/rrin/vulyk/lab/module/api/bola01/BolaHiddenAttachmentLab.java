package com.github.rrin.vulyk.lab.module.api.bola01;

import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.lab.domain.LabDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskHintDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnLabEnabled(BolaHiddenAttachmentLab.LAB_ID)
public class BolaHiddenAttachmentLab implements LabDefinition {

    public static final String LAB_ID = "BOLA-01";
    public static final String TASK_ID = "extract-private-attachment-flag";

    @Override
    public String getId() {
        return LAB_ID;
    }

    @Override
    public String getTitle() {
        return "Hidden Attachment BOLA";
    }

    @Override
    public String getCategory() {
        return "API Authorization";
    }

    @Override
    public String getDescription() {
        return "Enumerate attachment object IDs on the core file download route and access a private file without ownership checks.";
    }

    @Override
    public String getEntryPath() {
        return "/web/labs/" + LAB_ID;
    }

    @Override
    public List<LabTaskDefinition> getTasks() {
        return List.of(
            new LabTaskDefinition(
                TASK_ID,
                "Extract private attachment flag",
                "Use object-id enumeration against GET /files/{id} and submit the recovered flag.",
                100,
                LabTaskMode.FLAG_SUBMISSION,
                List.of(
                    new LabTaskHintDefinition(
                        "enumerate-ids",
                        "Hint 1",
                        "Try requesting sequential numeric IDs from /files/{id} while authenticated as a regular user.",
                        10
                    )
                )
            )
        );
    }
}
