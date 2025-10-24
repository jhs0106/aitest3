package edu.sm.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
@RequestMapping("/ai2")
public class Ai2Controller {

    String dir = "ai2/";

    @RequestMapping("")
    public String main(Model model){
        model.addAttribute("center", dir+"center");
        model.addAttribute("left", dir+"left");
        return "index";
    }

    @RequestMapping("/smart-home")
    public String smartHome(Model model){
        model.addAttribute("center", dir+"smart-home");
        model.addAttribute("left", dir+"left");
        return "index";
    }
}
