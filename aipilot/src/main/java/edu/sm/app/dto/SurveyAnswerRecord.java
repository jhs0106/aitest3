package edu.sm.app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SurveyAnswerRecord {
    private String questionId;
    private String questionText;
    private String selectedOptionId;
    private String selectedOptionText;
    private Integer score;

    public SurveyAnswerRecord() {
    }

    public SurveyAnswerRecord(String questionId,
                              String questionText,
                              String selectedOptionId,
                              String selectedOptionText,
                              Integer score) {
        this.questionId = questionId;
        this.questionText = questionText;
        this.selectedOptionId = selectedOptionId;
        this.selectedOptionText = selectedOptionText;
        this.score = score;
    }

    public String getQuestionId() {
        return questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getSelectedOptionId() {
        return selectedOptionId;
    }

    public String getSelectedOptionText() {
        return selectedOptionText;
    }

    public Integer getScore() {
        return score;
    }
}