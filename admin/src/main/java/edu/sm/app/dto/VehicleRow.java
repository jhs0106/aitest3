package edu.sm.app.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VehicleRow {
    private String plate;
    private String size;
    private String color;
    private LocalDateTime lastWashAt;
}
