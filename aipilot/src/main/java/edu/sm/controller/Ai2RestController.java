package edu.sm.controller;

import edu.sm.app.service.Ai2IntegratedService;
import edu.sm.app.service.TrialService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ai2/api")
@Slf4j
@RequiredArgsConstructor
public class Ai2RestController {

    private final Ai2IntegratedService ai2IntegratedService;
    private final TrialService trialService;

    // ===== 기존 IoT 관련 엔드포인트 =====
    @GetMapping("/sensor-data")
    public Map<String, Object> getSensorData() {
        return ai2IntegratedService.getSensorData();
    }

    @PostMapping("/voice-control")
    public Map<String, String> voiceControl(
            @RequestParam("speech") MultipartFile speech,
            HttpSession session) throws IOException {
        log.info("음성 명령 처리");
        return ai2IntegratedService.processVoiceCommand(speech, session.getId());
    }

    @PostMapping("/text-control")
    public Map<String, String> textControl(
            @RequestParam("command") String command,
            HttpSession session) {
        log.info("텍스트 명령: {}", command);
        return ai2IntegratedService.processTextCommand(command, session.getId());
    }

    @PostMapping("/rag-search")
    public Map<String, String> ragSearch(@RequestParam("question") String question) {
        log.info("RAG 검색: {}", question);
        String answer = ai2IntegratedService.searchManual(question);
        return Map.of("answer", answer);
    }

    @GetMapping(value = "/memory-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> memoryChat(
            @RequestParam("message") String message,
            HttpSession session) {
        log.info("Memory 채팅: {}", message);
        return ai2IntegratedService.chatWithMemory(message, session.getId());
    }

    @PostMapping("/upload-document")
    public String uploadDocument(
            @RequestParam("attach") MultipartFile attach,
            @RequestParam("type") String type) throws IOException {
        log.info("문서 업로드: {}", attach.getOriginalFilename());
        return ai2IntegratedService.uploadDocument(attach, type);
    }

    @PostMapping("/clear-vector")
    public String clearVector() {
        ai2IntegratedService.clearVectorStore();
        return "벡터 저장소를 초기화했습니다.";
    }

    @GetMapping("/device-status")
    public Map<String, Object> getDeviceStatus() {
        return ai2IntegratedService.getDeviceStatus();
    }

    // ===== 모의 법정 관련 엔드포인트 =====

    /**
     * 기본 재판 채팅 (기존 메서드 유지)
     */
    @GetMapping(value = "/trial-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> trialChat(
            @RequestParam("message") String message,
            @RequestParam(value = "role", required = false) String role) {
        log.info("모의 법정 채팅 - 역할: {}, 메시지: {}", role, message);
        return trialService.chat(message, role);
    }

    /**
     * Memory 기반 재판 (대화 기록 유지)
     */
    @GetMapping(value = "/trial-memory-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> trialMemoryChat(
            @RequestParam("message") String message,
            HttpSession session) {
        log.info("Memory 재판 - 세션ID: {}, 메시지: {}", session.getId(), message);
        return trialService.chatWithMemory(message, session.getId());
    }

    /**
     * RAG 기반 법률 자문
     */
    @GetMapping(value = "/legal-advice", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> legalAdvice(
            @RequestParam("question") String question,
            @RequestParam(value = "lawType", required = false) String lawType) {
        log.info("법률 자문 - 유형: {}, 질문: {}", lawType, question);
        return trialService.getLegalAdvice(question, lawType);
    }

    /**
     * Memory + RAG 통합 재판
     */
    @GetMapping(value = "/trial-full", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> trialFull(
            @RequestParam("message") String message,
            @RequestParam(value = "lawType", required = false) String lawType,
            HttpSession session) {
        log.info("통합 재판 - 세션ID: {}, 법률유형: {}, 메시지: {}",
                session.getId(), lawType, message);
        return trialService.chatWithMemoryAndRag(message, session.getId(), lawType);
    }

    /**
     * AI 자동 진행 - 검사/변호사 자동 발언
     */
    @GetMapping(value = "/trial-ai-proceed", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> trialAiProceed(
            @RequestParam(value = "sessionId", required = false) String sessionId,
            HttpSession session) {
        String sid = sessionId != null ? sessionId : session.getId();
        log.info("AI 자동 진행 - 세션ID: {}", sid);
        return trialService.aiAutoProceed(sid);
    }

    /**
     * 판결 생성 - Memory + RAG 기반
     */
    @GetMapping(value = "/trial-verdict", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> trialVerdict(
            @RequestParam(value = "sessionId", required = false) String sessionId,
            HttpSession session) {
        String sid = sessionId != null ? sessionId : session.getId();
        log.info("판결 생성 - 세션ID: {}", sid);
        return trialService.generateVerdict(sid);
    }

    /**
     * 재판 종료
     */
    @PostMapping("/trial-complete")
    public Map<String, Object> trialComplete(
            @RequestParam(value = "sessionId", required = false) String sessionId,
            HttpSession session) {
        String sid = sessionId != null ? sessionId : session.getId();
        log.info("재판 종료 - 세션ID: {}", sid);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "재판이 종료되었습니다.");
        response.put("sessionId", sid);

        return response;
    }

    /**
     * 역할 전환
     */
    @PostMapping("/trial-switch-role")
    public Map<String, Object> trialSwitchRole(
            @RequestParam("role") String role,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            HttpSession session) {
        String sid = sessionId != null ? sessionId : session.getId();
        log.info("역할 전환 - 세션ID: {}, 새 역할: {}", sid, role);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("newRole", role);
        response.put("message", "역할이 " + role + "로 전환되었습니다.");

        return response;
    }
}