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
                         @RequestParam("zone") String zone,
                         @RequestParam("attach") MultipartFile attach) throws Exception {
        return etlService.ingestManual(scenario, zone, attach);
    }

    @PostMapping("/clear")
    public String clearVectorStore() {
        etlService.clearAll();
        return "괴담 규칙 벡터 저장소를 초기화했습니다.";
    }

    @GetMapping("/scenarios")
    public List<String> scenarios() {
        return etlService.listScenarios();
    }

    @PostMapping("/start")
    public ManualStartResponse start(@RequestParam(value = "scenario", required = false) String scenario,
                                     @RequestParam(value = "zone", required = false) String zone,
                                     @RequestParam(value = "rumor", required = false) String rumor) {
        return ragService.startScenario(scenario, zone, rumor);
    }

    @PostMapping("/ask")
    public ManualAnswerResponse ask(@RequestParam("conversationId") String conversationId,
                                    @RequestParam("question") String question,
                                    @RequestParam(value = "scenario", required = false) String scenario,
                                    @RequestParam(value = "zone", required = false) String zone,
                                    @RequestParam(value = "rumor", required = false) String rumor) {
        return ragService.answerManual(conversationId, question, scenario, zone, rumor);
    }
}