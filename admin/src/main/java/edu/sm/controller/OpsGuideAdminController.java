package edu.sm.controller;

import edu.sm.app.service.OpsGuideRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai6/admin/ops-guide")
@RequiredArgsConstructor
@Slf4j
public class OpsGuideAdminController {

    private final OpsGuideRagService opsGuideRagService;

    /**
     * 운영문서 RAG 업로드
     * form-data: title, attach(file)
     */
    @PostMapping("/upload")
    public String upload(
            @RequestParam("title") String title,
            @RequestParam("attach") MultipartFile attach
    ) throws Exception {
        log.info("[OPS-GUIDE-UPLOAD] title={}, file={}", title, attach.getOriginalFilename());
        return opsGuideRagService.ingestOpsGuide(title, attach);
    }

    /**
     * 최근 업로드된 ops_guide chunks 미리보기
     */
    @GetMapping("/recent")
    public List<Map<String,Object>> recent() {
        return opsGuideRagService.listRecentOpsGuide(20);
    }

    /**
     * (테스트용) 순수 ops_guide RAG에만 질문해 보기
     */
    @PostMapping("/ask")
    public Flux<String> ask(@RequestParam("q") String q) {
        return opsGuideRagService.askOpsGuideOnly(q);
    }
}
