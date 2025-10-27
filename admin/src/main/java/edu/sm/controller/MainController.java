package edu.sm.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
public class MainController {

    @RequestMapping("/")
    public String main(Model model) {
        return "index";
    }

    @RequestMapping("/gatelog")
    public String gatelog(Model model) throws Exception {
        model.addAttribute("center", "gatelog");
        return "index";
    }
    @RequestMapping("/washorder")
    public String washorder(Model model) throws Exception {
        model.addAttribute("center", "washorder");
        return "index";
    }
    @RequestMapping("/vehicle")
    public String vehicle(Model model) throws Exception {
        model.addAttribute("center", "vehicle");
        return "index";
    }
    @RequestMapping("/ownerreport")
    public String ownerreport(Model model) throws Exception {
        model.addAttribute("center", "ownerreport");
        return "index";
    }

    @RequestMapping("/opsguideadmin")
    public String opsguideadmin(Model model) throws Exception {
        model.addAttribute("center", "opsguideadmin");
        return "index";
    }

}