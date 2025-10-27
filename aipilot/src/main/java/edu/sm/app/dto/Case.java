package edu.sm.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사건 DTO (aipilot에서 사용)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Case {

    private Integer caseId;              // 사건 ID (PK)
    private String caseNumber;           // 사건번호 (예: 2025고단1234)
    private String caseType;             // 사건 유형 (criminal/civil)
    private String defendant;            // 피고인
    private String charge;               // 혐의/청구 내용
    private String description;          // 상세 설명
    private String status;               // 상태 (registered/in_progress/closed)
    private String verdict;              // 판결 (판결 전: null)
    private LocalDateTime createdAt;     // 등록일시
    private LocalDateTime updatedAt;     // 수정일시
}