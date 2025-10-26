package edu.sm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/hauntedmanual")
public class HauntedManualPageController {

    private final String dir = "hauntedmanual/";

    @GetMapping("")
    public String manualRoot() {
        return "redirect:/hauntedmanual/setup";
    }

    @GetMapping("/setup")
    public String setup(Model model) {
        model.addAttribute("center", dir + "setup");
        model.addAttribute("left", dir + "left");
        return "index";
    }

    @GetMapping("/duty")
    public String duty(Model model) {
        model.addAttribute("center", dir + "duty");
        model.addAttribute("left", dir + "left");
        return "index";
    }
}