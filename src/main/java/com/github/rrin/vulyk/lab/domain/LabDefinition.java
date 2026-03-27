package com.github.rrin.vulyk.lab.domain;

import java.util.List;

public interface LabDefinition {

    String getId();

    String getTitle();

    String getCategory();

    String getDescription();

    String getEntryPath();

    List<LabTaskDefinition> getTasks();

    default int getTotalPoints() {
        return getTasks().stream()
            .mapToInt(LabTaskDefinition::points)
            .sum();
    }
}