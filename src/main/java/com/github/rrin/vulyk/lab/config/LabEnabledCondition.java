package com.github.rrin.vulyk.lab.config;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class LabEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Object rawLabId = metadata.getAnnotationAttributes(ConditionalOnLabEnabled.class.getName())
            .get("value");

        if (!(rawLabId instanceof String labId) || labId.isBlank()) {
            return false;
        }

        String configuredLabs = context.getEnvironment().getProperty("lab.enabled", "");
        Set<String> enabledLabs = Arrays.stream(configuredLabs.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .map(value -> value.toUpperCase(Locale.ROOT))
            .collect(Collectors.toSet());

        return enabledLabs.contains(labId.trim().toUpperCase(Locale.ROOT));
    }
}