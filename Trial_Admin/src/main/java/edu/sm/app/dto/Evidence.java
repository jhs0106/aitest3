package edu.sm.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 증거 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evidence {

    private Integer evidenceId;          // 증거 ID (PK)
    private Integer caseId;              // 사건 ID (FK)
    private String evidenceType;         // 증거 유형 (document/image/video/audio)
    private String fileName;             // 파일명
    private String filePath;             // 파일 저장 경로
    private Long fileSize;               // 파일 크기 (bytes)
    private String extractedText;        // 추출된 텍스트 (OCR/PDF)
    private String description;          // 증거 설명
    private LocalDateTime uploadedAt;    // 업로드 일시
}