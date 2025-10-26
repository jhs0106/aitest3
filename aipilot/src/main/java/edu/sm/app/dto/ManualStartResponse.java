package edu.sm.app.dto;

public class ManualStartResponse {
    private final String conversationId;
    private final String scenario;
    private final String manual;

    public ManualStartResponse(String conversationId, String scenario, String manual) {
        this.conversationId = conversationId;
        this.scenario = scenario;
        this.manual = manual;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getScenario() {
        return scenario;
    }

    public String getManual() {
        return manual;
    }
}