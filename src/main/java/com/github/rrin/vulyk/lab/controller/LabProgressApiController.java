package com.github.rrin.vulyk.lab.controller;

import com.github.rrin.vulyk.lab.domain.LabApiStatusResponse;
import com.github.rrin.vulyk.lab.service.LabApiSecurityService;
import com.github.rrin.vulyk.lab.service.LabProgressService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/labs")
@RequiredArgsConstructor
public class LabProgressApiController {

    private final LabApiSecurityService labApiSecurityService;
    private final LabProgressService labProgressService;

    @GetMapping
    public List<LabApiStatusResponse> listStatuses(
        @RequestHeader(name = "X-Lab-Api-Key", required = false) String apiKey
    ) {
        labApiSecurityService.verify(apiKey);
        return labProgressService.getAllStatuses();
    }

    @GetMapping("/{labId}")
    public LabApiStatusResponse getStatus(
        @PathVariable String labId,
        @RequestHeader(name = "X-Lab-Api-Key", required = false) String apiKey
    ) {
        labApiSecurityService.verify(apiKey);
        return labProgressService.getStatus(labId);
    }
}