package edu.sm.app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SurveyRecord {
    private String surveyId;
    private String clientId;
    private String category;
    private List<SurveyAnswerRecord> answers;
    private Integer totalScore;
    private Integer maxScore;
    private String resultLevel;
    private String resultSummary;
    private String resultRecommendation;
    private String selfReflection;
    private String submittedAt;

    public SurveyRecord() {
        this.answers = List.of();
    }

    public SurveyRecord(String surveyId,
                        String clientId,
                        String category,
                        List<SurveyAnswerRecord> answers,
                        Integer totalScore,
                        Integer maxScore,
                        String resultLevel,
                        String resultSummary,
                        String resultRecommendation,
                        String selfReflection,
                        String submittedAt) {
        this.surveyId = surveyId;
        this.clientId = clientId;
        this.category = category;
        this.answers = answers == null ? List.of() : List.copyOf(answers);
        this.totalScore = totalScore;
        this.maxScore = maxScore;
        this.resultLevel = resultLevel;
        this.resultSummary = resultSummary;
        this.resultRecommendation = resultRecommendation;
        this.selfReflection = selfReflection;
        this.submittedAt = submittedAt;
    }

    public String getSurveyId() {
        return surveyId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getCategory() {
        return category;
    }

    public List<SurveyAnswerRecord> getAnswers() {
        return answers;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public Integer getMaxScore() {
        return maxScore;
    }

    public String getResultLevel() {
        return resultLevel;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public String getResultRecommendation() {
        return resultRecommendation;
    }

    public String getSelfReflection() {
        return selfReflection;
    }

    public String getSubmittedAt() {
        return submittedAt;
    }
}