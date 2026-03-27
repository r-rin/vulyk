package com.github.rrin.vulyk.lab.domain;

public record LabApiTaskStatus(
    String taskId,
    String title,
    String description,
    LabTaskMode mode,
    LabTaskProgressStatus status,
    int points,
    int maxPointsAvailable,
    int pointsEarned
) {
}