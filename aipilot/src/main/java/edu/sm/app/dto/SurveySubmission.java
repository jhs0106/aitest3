package edu.sm.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SurveySubmission(
        String clientId,
        String category,
        String sessionFocus,
        String keyObservations,
        String supportNeeds,
        String nextSteps
) {
}