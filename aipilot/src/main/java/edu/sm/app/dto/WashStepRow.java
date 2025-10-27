package edu.sm.app.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WashStepRow {
    private int stepIdx;
    private String stepName;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer pressureBar;
    private String chemCode;
    private String result;
}
