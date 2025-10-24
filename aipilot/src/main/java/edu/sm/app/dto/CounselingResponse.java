package edu.sm.app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CounselingResponse(
        String advice,
        List<SurveyRecord> references
) {
}