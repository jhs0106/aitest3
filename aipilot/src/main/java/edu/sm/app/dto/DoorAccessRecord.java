package edu.sm.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 출입 기록 DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoorAccessRecord {
    private Long id;
    private String name;
    private String status; // 성공/실패
    private LocalDateTime accessTime;
}