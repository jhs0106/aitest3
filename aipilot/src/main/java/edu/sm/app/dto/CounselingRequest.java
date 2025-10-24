package edu.sm.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CounselingRequest(
        String clientId,
        String category,
        String question
) {
}