package edu.sm.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 법률 문서 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LawDoc {

    private Integer docId;               // 문서 ID (PK)
    private String fileName;             // 파일명
    private String lawType;              // 법률 유형 (criminal_law/civil_law)
    private String filePath;             // 파일 저장 경로
    private Long fileSize;               // 파일 크기 (bytes)
    private Integer chunkCount;          // 분할된 청크 수
    private String etlStatus;            // ETL 상태 (pending/processing/completed/failed)
    private LocalDateTime uploadedAt;    // 업로드 일시
    private LocalDateTime processedAt;   // 처리 완료 일시
}