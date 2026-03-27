package com.github.rrin.vulyk.lab.domain;

import java.util.List;

public record LabApiStatusResponse(
    String labId,
    String title,
    String category,
    LabProgressStatus status,
    int earnedPoints,
    int totalPoints,
    List<LabApiTaskStatus> tasks
) {
}