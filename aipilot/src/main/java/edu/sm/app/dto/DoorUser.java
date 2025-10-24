package edu.sm.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 등록된 사용자 정보 DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoorUser {
    private Long id;
    private String name;
    // AI가 분석한 얼굴 특징을 텍스트로 저장 (예: "안경을 쓴 금발의 남성")
    private String faceSignature;
}