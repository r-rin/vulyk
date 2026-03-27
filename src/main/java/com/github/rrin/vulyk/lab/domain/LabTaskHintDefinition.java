package com.github.rrin.vulyk.lab.domain;

public record LabTaskHintDefinition(
    String id,
    String title,
    String content,
    int penalty
) {
}