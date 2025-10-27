package edu.sm.controller;

import edu.sm.app.service.LawDocEtlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Trial_Admin - 법률 문서 REST API 컨트롤러
 *
 * 업로드된 법률 문서를 추출/분할하여 pgvector에 적재하고 관리한다.
 */
@RestController
@RequestMapping("/api/lawdoc")
@Slf4j
@RequiredArgsConstructor
public class LawDocRestController {

    private final LawDocEtlService lawDocEtlService;

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

        log.info("법률 문서 업로드 - 파일: {}, 유형: {}", file != null ? file.getOriginalFilename() : "", lawType);

        try {
            return lawDocEtlService.ingest(lawType, file);
        } catch (IllegalArgumentException e) {
            log.warn("업로드 검증 실패 - {}", e.getMessage());
            return "❌ " + e.getMessage();
        } catch (IOException e) {
            log.error("문서 추출 실패", e);
            return "❌ 문서 처리 중 오류가 발생했습니다: " + e.getMessage();
        } catch (Exception e) {
            log.error("업로드 실패", e);
            return "❌ 업로드 중 알 수 없는 오류가 발생했습니다.";
        }
    }

    /**
     * VectorStore 초기화
     */
    @PostMapping("/clear-vector")
    public String clearVector() {
        log.info("VectorStore 초기화 요청");

        lawDocEtlService.clearVectorStore();
        return "✅ VectorStore가 초기화되었습니다.";
    }

    /**
     * VectorStore 통계 조회
     */
    @GetMapping("/vector-stats")
    public Map<String, Object> getVectorStats() {
        log.info("VectorStore 통계 조회");

        return lawDocEtlService.getVectorStats();
    }
}