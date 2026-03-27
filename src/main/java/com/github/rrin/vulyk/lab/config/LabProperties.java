package com.github.rrin.vulyk.lab.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "lab")
public class LabProperties {

    private List<String> enabled = new ArrayList<>();
    private Api api = new Api();
    private Validation validation = new Validation();

    @Getter
    @Setter
    public static class Api {
        private String key;
    }

    @Getter
    @Setter
    public static class Validation {
        private String flagPrefix = "flag{";
        private String flagSuffix = "}";
    }
}