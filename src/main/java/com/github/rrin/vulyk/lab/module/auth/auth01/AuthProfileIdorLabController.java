package com.github.rrin.vulyk.lab.module.auth.auth01;

import com.github.rrin.vulyk.lab.config.ConditionalOnLabEnabled;
import com.github.rrin.vulyk.lab.service.LabProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@ConditionalOnLabEnabled(AuthProfileIdorLab.LAB_ID)
public class AuthProfileIdorLabController {

    private final AuthProfileIdorLab labDefinition;
    private final LabProgressService labProgressService;

    @GetMapping("/web/labs/" + AuthProfileIdorLab.LAB_ID)
    public String taskPage(Model model) {
        model.addAttribute("lab", labDefinition);
        model.addAttribute("labProgress", labProgressService.getLabCard(labDefinition.getId()));
        return "lab/auth-profile-idor";
    }

    @GetMapping("/labs/" + AuthProfileIdorLab.LAB_ID)
    public String legacyLabRoute() {
        return "redirect:" + labDefinition.getEntryPath();
    }

    @GetMapping("/tasks/" + AuthProfileIdorLab.LAB_ID)
    public String legacyTasksRoute() {
        return "redirect:" + labDefinition.getEntryPath();
    }

    @PostMapping("/web/labs/" + AuthProfileIdorLab.LAB_ID + "/tasks/{taskId}/hints/{hintId}")
    public String revealHint(
        @PathVariable String taskId,
        @PathVariable String hintId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            labProgressService.revealHint(labDefinition.getId(), taskId, hintId);
            redirectAttributes.addFlashAttribute("notice", "Hint revealed. The task's maximum obtainable score was adjusted.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:" + labDefinition.getEntryPath();
    }
}
