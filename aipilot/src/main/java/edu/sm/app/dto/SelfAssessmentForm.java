package edu.sm.app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SelfAssessmentForm {
    private String category;
    private String title;
    private String description;
    private List<SelfAssessmentQuestion> questions;
    private List<SelfAssessmentGuide> guides;
    private int maxScore;

    public SelfAssessmentForm() {
        this.questions = List.of();
        this.guides = List.of();
    }

    private SelfAssessmentForm(String category,
                               String title,
                               String description,
                               List<SelfAssessmentQuestion> questions,
                               List<SelfAssessmentGuide> guides,
                               int maxScore) {
        this.category = category;
        this.title = title;
        this.description = description;
        this.questions = questions == null ? List.of() : List.copyOf(questions);
        this.guides = guides == null ? List.of() : List.copyOf(guides);
        this.maxScore = maxScore;
    }

    public static SelfAssessmentForm create(String category,
                                            String title,
                                            String description,
                                            List<SelfAssessmentQuestion> questions,
                                            List<SelfAssessmentGuide> guides) {
        List<SelfAssessmentQuestion> questionCopy = questions == null ? List.of() : List.copyOf(questions);
        List<SelfAssessmentGuide> guideCopy = guides == null ? List.of() : List.copyOf(guides);

        int computedMaxScore = questionCopy.stream()
                .map(SelfAssessmentQuestion::getOptions)
                .filter(Objects::nonNull)
                .mapToInt(options -> options.stream()
                        .map(SelfAssessmentOption::getScore)
                        .filter(Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .max()
                        .orElse(0))
                .sum();

        return new SelfAssessmentForm(
                category,
                title,
                description,
                questionCopy,
                guideCopy,
                computedMaxScore
        );
    }

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<SelfAssessmentQuestion> getQuestions() {
        return questions;
    }

    public List<SelfAssessmentGuide> getGuides() {
        return guides;
    }

    public int getMaxScore() {
        return maxScore;
    }
}