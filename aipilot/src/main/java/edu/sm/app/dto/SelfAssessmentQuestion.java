package edu.sm.app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SelfAssessmentQuestion {
    private String id;
    private String category;
    private String text;
    private List<SelfAssessmentOption> options;

    public SelfAssessmentQuestion() {
        this.options = List.of();
    }

    public SelfAssessmentQuestion(String id,
                                  String category,
                                  String text,
                                  List<SelfAssessmentOption> options) {
        this.id = id;
        this.category = category;
        this.text = text;
        this.options = options == null ? List.of() : List.copyOf(options);
    }

    public String getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getText() {
        return text;
    }

    public List<SelfAssessmentOption> getOptions() {
        return options;
    }
}