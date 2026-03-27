package com.github.rrin.vulyk.lab.domain;

public record LabTaskDefinition(
    String id,
    String title,
    String description,
    int points,
    LabTaskMode mode,
    java.util.List<LabTaskHintDefinition> hints
) {

    public LabTaskDefinition(
        String id,
        String title,
        String description,
        int points,
        LabTaskMode mode
    ) {
        this(id, title, description, points, mode, java.util.List.of());
    }
}