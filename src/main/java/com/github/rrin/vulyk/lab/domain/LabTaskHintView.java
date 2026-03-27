package com.github.rrin.vulyk.lab.domain;

public record LabTaskHintView(
    String id,
    String title,
    String content,
    int penalty,
    boolean revealed
) {
}