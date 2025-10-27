package edu.sm.app.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GateLogRow {
    private Long id;
    private String plate;
    private String eventType;
    private LocalDateTime loggedAt;
}
