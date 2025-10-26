package edu.sm.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Trial_Admin - 법률 문서 관리 컨트롤러
 */
@Controller
@Slf4j
@RequestMapping("/lawdoc")
public class LawDocController {

    String dir = "lawdoc/";

    /**
     * 문서 업로드 화면 (RAG ETL)
     */
    @RequestMapping("/upload")
    public String upload(Model model) {
        model.addAttribute("center", dir + "upload");
        model.addAttribute("left", "left");
        return "index";
    }

    /**
     * 문서 목록 화면
     */
    @RequestMapping("/list")
    public String list(Model model) {
        model.addAttribute("center", dir + "list");
        model.addAttribute("left", "left");
        return "index";
    }

    /**
     * VectorStore 관리 화면
     */
    @RequestMapping("/vector")
    public String vector(Model model) {
        model.addAttribute("center", dir + "vector");
        model.addAttribute("left", "left");
        return "index";
    }
}