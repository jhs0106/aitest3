package edu.sm.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CounselingRequest {
    private String clientId;
    private String category;
    private String question;

    public CounselingRequest() {
    }

    public CounselingRequest(String clientId, String category, String question) {
        this.clientId = clientId;
        this.category = category;
        this.question = question;
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

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}