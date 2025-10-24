package edu.sm.app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SurveyRecord(
        String surveyId,
        String clientId,
        String category,
        String sessionFocus,
        String keyObservations,
        String supportNeeds,
        String nextSteps,
        String submittedAt
) {
}