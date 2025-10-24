package edu.sm.app.dto;

public class SelfAssessmentOption {
    private String id;
    private String text;
    private Integer score;

    public SelfAssessmentOption() {
    }

    public SelfAssessmentOption(String id, String text, Integer score) {
        this.id = id;
        this.text = text;
        this.score = score;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public Integer getScore() {
        return score;
    }
}