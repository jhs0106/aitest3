package edu.sm.app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SelfAssessmentGuide {
    private String level;
    private int minScore;
    private int maxScore;
    private String summary;
    private String recommendation;

    public SelfAssessmentGuide() {
    }

    public SelfAssessmentGuide(String level,
                               int minScore,
                               int maxScore,
                               String summary,
                               String recommendation) {
        this.level = level;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.summary = summary;
        this.recommendation = recommendation;
    }

    public String getLevel() {
        return level;
    }

    public int getMinScore() {
        return minScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public String getSummary() {
        return summary;
    }

    public String getRecommendation() {
        return recommendation;
    }
}