package edu.sm.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
@RequestMapping("/springai3")
public class SpringAI3Controller {

    String dir = "springai3/";

    @RequestMapping("")
    public String main(Model model) {
        model.addAttribute("center", dir+"center");
        model.addAttribute("left", dir+"left");
        return "index";
    }
    @RequestMapping("/ai1")
    public String ai1(Model model) {
        model.addAttribute("center", dir+"ai1");
        model.addAttribute("left", dir+"left");
        return "index";
    }
    @RequestMapping("/ai2")
    public String ai2(Model model) {
        model.addAttribute("center", dir+"ai2");
        model.addAttribute("left", dir+"left");
        return "index";
    }
    @RequestMapping("/ai3")
    public String ai3(Model model) {
        model.addAttribute("center", dir+"ai3");
        model.addAttribute("left", dir+"left");
        return "index";
    }
    
}