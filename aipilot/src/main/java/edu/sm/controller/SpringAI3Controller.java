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
    @RequestMapping("/carwash_entry")
    public String carwash_entry(Model model) {
        model.addAttribute("center", dir+"carwash_entry");
        model.addAttribute("left", dir+"left");
        return "index";
    }
    @RequestMapping("/carwash_plan")
    public String carwash_plan(Model model) {
        model.addAttribute("center", dir+"carwash_plan");
        model.addAttribute("left", dir+"left");
        return "index";
    }
    @RequestMapping("/carwash_progress")
    public String carwash_progress(Model model) {
        model.addAttribute("center", dir+"carwash_progress");
        model.addAttribute("left", dir+"left");
        return "index";
    }
    @RequestMapping("/gatelog")
    public String gatelog(Model model) {
        model.addAttribute("center", dir+"gatelog");
        model.addAttribute("left", dir+"left");
        return "index";
    }
    @RequestMapping("/vehicle")
    public String vehicle(Model model) {
        model.addAttribute("center", dir+"vehicle");
        model.addAttribute("left", dir+"left");
        return "index";
    }
    @RequestMapping("/washorder")
    public String washorder(Model model) {
        model.addAttribute("center", dir+"washorder");
        model.addAttribute("left", dir+"left");
        return "index";
    }
    
}