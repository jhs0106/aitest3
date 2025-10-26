package edu.sm.app.dto;

public class ManualAnswerResponse {
    private final String message;

    public ManualAnswerResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}