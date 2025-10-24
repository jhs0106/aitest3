package edu.sm.app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CounselingResponse {
    private String advice;
    private List<SurveyRecord> references;

    public CounselingResponse() {
        this.references = List.of();
    }

    public CounselingResponse(String advice, List<SurveyRecord> references) {
        this.advice = advice;
        this.references = references == null ? List.of() : List.copyOf(references);
    }

    public String getAdvice() {
        return advice;
    }

    public void setAdvice(String advice) {
        this.advice = advice;
    }

    public List<SurveyRecord> getReferences() {
        return references;
    }

    public void setReferences(List<SurveyRecord> references) {
        this.references = references == null ? List.of() : List.copyOf(references);
    }
}