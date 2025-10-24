package edu.sm.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SurveySubmission {
    private String clientId;
    private String category;
    private List<SurveyAnswerSubmission> answers;
    private String selfReflection;

    public SurveySubmission() {
        this.answers = List.of();
    }

    public SurveySubmission(String clientId,
                            String category,
                            List<SurveyAnswerSubmission> answers,
                            String selfReflection) {
        this.clientId = clientId;
        this.category = category;
        this.answers = answers == null ? List.of() : List.copyOf(answers);
        this.selfReflection = selfReflection;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<SurveyAnswerSubmission> getAnswers() {
        return answers;
    }

    public void setAnswers(List<SurveyAnswerSubmission> answers) {
        this.answers = answers == null ? List.of() : List.copyOf(answers);
    }

    public String getSelfReflection() {
        return selfReflection;
    }

    public void setSelfReflection(String selfReflection) {
        this.selfReflection = selfReflection;
    }
}