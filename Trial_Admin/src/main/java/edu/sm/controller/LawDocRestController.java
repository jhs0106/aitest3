package edu.sm.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Trial_Admin - 법률 문서 REST API 컨트롤러
 *
 * TODO: aipilot의 ETLService와 연동하여 실제 VectorStore에 저장
 */
@RestController
@RequestMapping("/api/lawdoc")
@Slf4j
public class LawDocRestController {

    /**
     * 법률 문서 업로드 (ETL)
     *
     * @param file 업로드 파일 (PDF, TXT, DOCX)
     * @param lawType 법률 유형 (criminal_law/civil_law)
     * @return 처리 결과 메시지
     */
    @PostMapping("/upload")
    public String uploadLawDoc(
            @RequestParam("file") MultipartFile file,
            @RequestParam("lawType") String lawType) {

        log.info("법률 문서 업로드 - 파일: {}, 유형: {}", file.getOriginalFilename(), lawType);

        try {
            // TODO: aipilot의 ETLService 호출하여 VectorStore에 저장
            // 예: restTemplate.postForObject("https://localhost:8445/api/etl", ...)

            return String.format("✅ %s 파일이 성공적으로 업로드되었습니다. (유형: %s)",
                    file.getOriginalFilename(), lawType);

        } catch (Exception e) {
            log.error("업로드 실패", e);
            return "❌ 업로드 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    /**
     * VectorStore 초기화
     */
    @PostMapping("/clear-vector")
    public String clearVector() {
        log.info("VectorStore 초기화 요청");

        try {
            // TODO: aipilot API 호출
            return "✅ VectorStore가 초기화되었습니다.";
        } catch (Exception e) {
            log.error("초기화 실패", e);
            return "❌ 초기화 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    /**
     * VectorStore 통계 조회
     */
    @GetMapping("/vector-stats")
    public String getVectorStats() {
        log.info("VectorStore 통계 조회");

        // TODO: aipilot API 호출하여 실제 통계 조회
        return """
            {
                "totalDocuments": 152,
                "criminalLaw": 78,
                "civilLaw": 74,
                "totalEmbeddings": 1203
            }
            """;
    }
}