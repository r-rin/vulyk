package com.github.rrin.vulyk.lab.controller;

import com.github.rrin.vulyk.lab.service.LabProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class LabPageController {

    private final LabProgressService labProgressService;

    @GetMapping("/web/labs")
    public String labs(Model model) {
        model.addAttribute("taskCatalog", labProgressService.getBoardView());
        return "web/labs";
    }

    @GetMapping("/tasks")
    public String tasksAlias() {
        return "redirect:/web/labs";
    }
}