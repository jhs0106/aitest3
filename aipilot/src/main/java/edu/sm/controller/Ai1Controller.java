package edu.sm.controller;

import edu.sm.app.dto.CounselingRequest;
import edu.sm.app.dto.CounselingResponse;
import edu.sm.app.dto.SurveyRecord;
import edu.sm.app.dto.SurveySubmission;
import edu.sm.app.service.SurveyRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Optional;

@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/ai1")
public class Ai1Controller {

    private static final String DIR = "ai1/";

    private final SurveyRagService surveyRagService;

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

    @PostMapping("/api/surveys")
    @ResponseBody
    public ResponseEntity<?> submitSurvey(@RequestBody SurveySubmission submission) {
        if (submission == null || !StringUtils.hasText(submission.clientId()) || !StringUtils.hasText(submission.category())) {
            return ResponseEntity.badRequest().body("clientId와 category는 필수 항목입니다.");
        }
        try {
            String surveyId = surveyRagService.storeSurvey(submission);
            return ResponseEntity.ok().body(surveyId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/api/surveys/{clientId}")
    @ResponseBody
    public ResponseEntity<List<SurveyRecord>> getSurveys(@PathVariable String clientId,
                                                         @RequestParam(name = "category", required = false) String category) {
        if (!StringUtils.hasText(clientId)) {
            return ResponseEntity.badRequest().build();
        }
        Optional<String> categoryOptional = StringUtils.hasText(category)
                ? Optional.of(category.trim())
                : Optional.empty();
        List<SurveyRecord> surveys = surveyRagService.findSurveys(clientId, categoryOptional);
        return ResponseEntity.ok(surveys);
    }

    @PostMapping("/api/counsel")
    @ResponseBody
    public ResponseEntity<CounselingResponse> counsel(@RequestBody CounselingRequest request) {
        if (request == null || !StringUtils.hasText(request.clientId()) || !StringUtils.hasText(request.question())) {
            return ResponseEntity.badRequest().build();
        }
        try {
            CounselingResponse response = surveyRagService.generateCounseling(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }
}