package edu.sm.controller;


import edu.sm.app.dto.ManualAnswerResponse;
import edu.sm.app.dto.ManualStartResponse;
import edu.sm.app.service.HauntedManualEtlService;
import edu.sm.app.service.HauntedManualRagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/haunted/manual")
@RequiredArgsConstructor
public class HauntedManualRestController {

    private final HauntedManualEtlService etlService;
    private final HauntedManualRagService ragService;

    @PostMapping("/upload")
    public String upload(@RequestParam("scenario") String scenario,
                         @RequestParam("attach") MultipartFile attach) throws Exception {
        return etlService.ingestManual(scenario, attach);
    }

    @PostMapping("/clear")
    public String clearVectorStore() {
        etlService.clearAll();
        return "괴담 규칙 벡터 저장소를 초기화했습니다.";
    }

    @GetMapping("/supported-types")
    public List<String> supportedTypes() {
        return etlService.supportedExtensions();
    }

    @PostMapping("/start")
    public ManualStartResponse start(@RequestParam(value = "scenario", required = false) String scenario,
                                     @RequestParam(value = "rumor", required = false) String rumor) {
        return ragService.startScenario(scenario, rumor);
    }

    @PostMapping("/ask")
    public ManualAnswerResponse ask(@RequestParam("conversationId") String conversationId,
                                    @RequestParam("question") String question,
                                    @RequestParam(value = "scenario", required = false) String scenario,
                                    @RequestParam(value = "rumor", required = false) String rumor) {
        return ragService.answerManual(conversationId, question, scenario, rumor);
    }
}