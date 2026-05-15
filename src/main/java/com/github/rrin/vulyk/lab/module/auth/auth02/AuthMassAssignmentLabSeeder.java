package com.github.rrin.vulyk.lab.module.auth.auth02;

import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.lab.service.LabProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnLabEnabled(AuthMassAssignmentLab.LAB_ID)
public class AuthMassAssignmentLabSeeder implements ApplicationRunner {

    private final AuthMassAssignmentLab labDefinition;
    private final LabProgressService labProgressService;

    @Override
    public void run(ApplicationArguments args) {
        labProgressService.ensureProgressRows(labDefinition);
    }
}
