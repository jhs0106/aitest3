package edu.sm.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
@RequestMapping("/ai1")
public class Ai1Controller {

    private static final String DIR = "ai1/";

    @GetMapping("")
    public String home(Model model) {
        model.addAttribute("center", DIR + "center");
        model.addAttribute("left", DIR + "left");
        return "index";
    }

    @GetMapping("/survey")
    public String surveyPage(Model model) {
        model.addAttribute("center", DIR + "survey");
        model.addAttribute("left", DIR + "left");
        return "index";
    }

    @GetMapping("/counsel")
    public String counselPage(Model model) {
        model.addAttribute("center", DIR + "counsel");
        model.addAttribute("left", DIR + "left");
        return "index";
    }
}