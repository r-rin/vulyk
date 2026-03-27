package com.github.rrin.vulyk.lab.domain;

import java.util.List;

public record LabBoardView(
    int totalPoints,
    int earnedPoints,
    int completedCount,
    int totalLabs,
    List<LabCardView> vulnerabilities
) {
}