package com.github.rrin.vulyk.lab.domain;

public record LabTaskView(
    String id,
    String title,
    String description,
    int points,
    int maxPointsAvailable,
    int pointsEarned,
    LabTaskMode mode,
    LabTaskProgressStatus status,
    java.util.List<LabTaskHintView> hints
) {
}