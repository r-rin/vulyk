package com.github.rrin.vulyk.lab.module.file.file01;

import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.lab.domain.LabDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskHintDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnLabEnabled(FileProfilePathTraversalLab.LAB_ID)
public class FileProfilePathTraversalLab implements LabDefinition {

    public static final String LAB_ID = "FILE-01";
    public static final String TASK_ID = "read-profile-traversal-flag";

    @Override
    public String getId() {
        return LAB_ID;
    }

    @Override
    public String getTitle() {
        return "Profile Image Path Traversal";
    }

    @Override
    public String getCategory() {
        return "File Handling";
    }

    @Override
    public String getDescription() {
        return "Exploit a vulnerable profile image viewer endpoint that resolves user-controlled file paths without proper traversal protection.";
    }

    @Override
    public String getEntryPath() {
        return "/web/labs/" + LAB_ID;
    }

    @Override
    public List<LabTaskDefinition> getTasks() {
        return List.of(new LabTaskDefinition(
            TASK_ID,
            "Read and submit traversal flag",
            "Use path traversal against profile image viewing to read a non-image internal file and submit the flag.",
            100,
            LabTaskMode.FLAG_SUBMISSION,
            List.of(
                new LabTaskHintDefinition(
                    "viewer-endpoint",
                    "Hint 1",
                    "Use GET /files/profile-images/view?name=... and try climbing out of the profile images directory.",
                    10
                ),
                new LabTaskHintDefinition(
                    "flag-shape",
                    "Hint 2",
                    "The recovered token follows the standard flag format: flag{...}.",
                    20
                )
            )
        ));
    }
}
