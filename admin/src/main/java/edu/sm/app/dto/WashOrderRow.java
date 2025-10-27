package edu.sm.app.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WashOrderRow {
    private String orderId;
    private String plate;
    private String status;
    private Integer price;
    private Integer etaMin;
    private LocalDateTime createdAt;

    private List<WashStepRow> steps; // wash_log에서 뽑은 단계들
}