package com.github.rrin.vulyk.lab.domain;

import java.util.List;

public record LabCardView(
    String id,
    String title,
    String category,
    String description,
    String entryPath,
    int points,
    int pointsEarned,
    LabProgressStatus status,
    boolean flagActivatable,
    boolean completed,
    List<LabTaskView> tasks
) {
}