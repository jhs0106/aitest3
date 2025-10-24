package edu.sm.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
@RequestMapping("/cctv")
public class AiCctvController {

    String dir = "cctv/";

    @RequestMapping("")
    public String main(Model model) {
        model.addAttribute("center", dir + "center");
        model.addAttribute("left", dir + "left");
        return "index";
    }

    @RequestMapping("/cctv")
    public String cctv(Model model) {
        model.addAttribute("center", dir + "cctv");
        model.addAttribute("left", dir + "left");
        return "index";
    }
}