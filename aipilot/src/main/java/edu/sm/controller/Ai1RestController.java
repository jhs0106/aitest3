package edu.sm.controller;

import edu.sm.app.dto.CounselingRequest;
import edu.sm.app.dto.CounselingResponse;
import edu.sm.app.dto.SelfAssessmentForm;
import edu.sm.app.dto.SelfAssessmentQuestion;
import edu.sm.app.dto.SurveyRecord;
import edu.sm.app.dto.SurveySubmission;
import edu.sm.app.service.SurveyRagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai1/api")
public class Ai1RestController {

    private final SurveyRagService surveyRagService;

    @GetMapping("/forms")
    public ResponseEntity<List<SelfAssessmentForm>> getForms(@RequestParam(name = "category", required = false) String category) {
        Optional<String> categoryOptional = StringUtils.hasText(category)
                ? Optional.of(category.trim())
                : Optional.empty();
        List<SelfAssessmentForm> forms = surveyRagService.getForms(categoryOptional);
        return ResponseEntity.ok(forms);
    }

    @GetMapping("/questions")
    public ResponseEntity<List<SelfAssessmentQuestion>> getQuestions(@RequestParam(name = "category", required = false) String category) {
        Optional<String> categoryOptional = StringUtils.hasText(category)
                ? Optional.of(category.trim())
                : Optional.empty();
        List<SelfAssessmentQuestion> questions = surveyRagService.getQuestions(categoryOptional);
        return ResponseEntity.ok(questions);
    }

    @PostMapping("/surveys")
    public ResponseEntity<?> submitSurvey(@RequestBody SurveySubmission submission) {
        if (submission == null || !StringUtils.hasText(submission.getClientId()) || !StringUtils.hasText(submission.getCategory())) {
            return ResponseEntity.badRequest().body("clientId와 category는 필수 항목입니다.");
        }
        try {
            String surveyId = surveyRagService.storeSurvey(submission);
            return ResponseEntity.ok().body(surveyId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/surveys/{clientId}")
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

    @PostMapping("/counsel")
    public ResponseEntity<CounselingResponse> counsel(@RequestBody CounselingRequest request) {
        if (request == null || !StringUtils.hasText(request.getClientId()) || !StringUtils.hasText(request.getQuestion())) {
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