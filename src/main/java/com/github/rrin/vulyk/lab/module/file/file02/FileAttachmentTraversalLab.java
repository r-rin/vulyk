package com.github.rrin.vulyk.lab.module.file.file02;

import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.lab.domain.LabDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskHintDefinition;
import com.github.rrin.vulyk.lab.domain.LabTaskMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnLabEnabled(FileAttachmentTraversalLab.LAB_ID)
public class FileAttachmentTraversalLab implements LabDefinition {

    public static final String LAB_ID = "FILE-02";
    public static final String TASK_FIND_VECTOR = "find-traversal-vector";
    public static final String TASK_READ_FLAG = "read-protected-lab-file";

    @Override
    public String getId() {
        return LAB_ID;
    }

    @Override
    public String getTitle() {
        return "Attachment Download Traversal";
    }

    @Override
    public String getCategory() {
        return "File Handling";
    }

    @Override
    public String getDescription() {
        return "Exploit traversal in attachment upload path handling to read a protected server file through core file routes.";
    }

    @Override
    public String getEntryPath() {
        return "/web/labs/" + LAB_ID;
    }

    @Override
    public List<LabTaskDefinition> getTasks() {
        return List.of(
            new LabTaskDefinition(
                TASK_FIND_VECTOR,
                "Find a traversal payload",
                "Upload an attachment via core routes using a traversal filename that escapes uploads/posts/{postId}.",
                40,
                LabTaskMode.STATE_TRACKED,
                List.of(
                    new LabTaskHintDefinition(
                        "dot-dot",
                        "Hint 1",
                        "Use POST /files/posts/{postId} with multipart filename containing ../ or ..\\ segments.",
                        10
                    )
                )
            ),
            new LabTaskDefinition(
                TASK_READ_FLAG,
                "Read and submit the seeded flag",
                "Retrieve the seeded training flag from uploads/flag.txt and submit it.",
                60,
                LabTaskMode.FLAG_SUBMISSION,
                List.of(
                    new LabTaskHintDefinition(
                        "target-location",
                        "Hint 1",
                        "After upload, use GET /files/{attachmentId}. The seeded target is uploads/flag.txt.",
                        15
                    )
                )
            )
        );
    }
}
