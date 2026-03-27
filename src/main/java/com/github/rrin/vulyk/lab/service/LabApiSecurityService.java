package com.github.rrin.vulyk.lab.service;

import com.github.rrin.vulyk.lab.config.LabProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class LabApiSecurityService {

    private static final String API_KEY_HEADER = "X-Lab-Api-Key";

    private final LabProperties labProperties;

    public void verify(String providedKey) {
        String configuredKey = labProperties.getApi().getKey();
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Lab API key is not configured");
        }

        if (providedKey == null || !configuredKey.equals(providedKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid " + API_KEY_HEADER);
        }
    }
}