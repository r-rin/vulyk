package com.github.rrin.vulyk.lab.module.jwt.jwt01;

import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.lab.service.LabProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnLabEnabled(JwtWeakSecretLab.LAB_ID)
public class JwtWeakSecretLabSeeder implements ApplicationRunner {

    private final JwtWeakSecretLab labDefinition;
    private final LabProgressService labProgressService;

    @Override
    public void run(ApplicationArguments args) {
        labProgressService.ensureProgressRows(labDefinition);
    }
}
