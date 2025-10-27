package edu.sm.controller;

import edu.sm.app.dto.Case;
import edu.sm.app.service.Ai2IntegratedService;
import edu.sm.app.service.CaseService;
import edu.sm.app.service.TrialService;
import edu.sm.app.service.TrialSessionManager;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai2/api")
@Slf4j
@RequiredArgsConstructor
public class Ai2RestController {

    private final Ai2IntegratedService ai2IntegratedService;
    private final TrialService trialService;
    private final CaseService caseService;
    private final TrialSessionManager sessionManager;

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
     * ⭐ 신규: 사건 목록 조회
     */
    @GetMapping("/trial-cases")
    public List<Case> getTrialCases() {
        try {
            log.info("사건 목록 조회");
            return caseService.getAllCases();
        } catch (Exception e) {
            log.error("사건 목록 조회 실패", e);
            return List.of();
        }
    }

    /**
     * ⭐ 신규: 특정 사건 조회
     */
    @GetMapping("/trial-case/{caseId}")
    public Case getTrialCase(@PathVariable Integer caseId) {
        try {
            log.info("사건 조회 - ID: {}", caseId);
            return caseService.getCaseById(caseId);
        } catch (Exception e) {
            log.error("사건 조회 실패 - ID: {}", caseId, e);
            return null;
        }
    }

    /**
     * ⭐ 신규: 사건 기반 재판 시작
     */
    @GetMapping(value = "/trial-start", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> startTrialWithCase(
            @RequestParam("caseId") Integer caseId,
            HttpSession session) {

        String sessionId = session.getId();
        log.info("사건 기반 재판 시작 - 사건ID: {}, 세션: {}", caseId, sessionId);

        try {
            Case trialCase = caseService.getCaseById(caseId);
            if (trialCase == null) {
                return Flux.just("오류: 사건을 찾을 수 없습니다.");
            }

            // 사건 상태를 'in_progress'로 업데이트
            trialCase.setStatus("in_progress");
            caseService.updateCase(trialCase);

            return trialService.startTrialWithCase(trialCase, sessionId);

        } catch (Exception e) {
            log.error("재판 시작 실패", e);
            return Flux.just("오류: 재판을 시작할 수 없습니다. " + e.getMessage());
        }
    }

    /**
     * 기본 재판 채팅 (기존 호환성 유지)
     */
    @GetMapping(value = "/trial-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> trialChat(
            @RequestParam("message") String message,
            @RequestParam(value = "role", required = false) String role,
            HttpSession session) {

        String sessionId = session.getId();
        log.info("재판 채팅 - 세션: {}, 역할: {}, 메시지: {}", sessionId, role, message);

        // 세션에 사건이 설정되어 있으면 Memory 기반 사용
        if (sessionManager.hasCase(sessionId)) {
            return trialService.chatWithMemory(message, sessionId);
        } else {
            // 사건 없이 단순 채팅
            return trialService.chat(message, role);
        }
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
    public Flux<String> trialAiProceed(HttpSession session) {
        String sessionId = session.getId();
        log.info("AI 자동 진행 - 세션ID: {}", sessionId);
        return trialService.aiAutoProceed(sessionId);
    }

    /**
     * 판결 생성 - Memory + RAG 기반
     */
    @GetMapping(value = "/trial-verdict", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> trialVerdict(HttpSession session) {
        String sessionId = session.getId();
        log.info("판결 생성 - 세션ID: {}", sessionId);
        return trialService.generateVerdict(sessionId);
    }

    /**
     * ⭐ 개선: 역할 전환
     */
    @PostMapping("/trial-switch-role")
    public Map<String, Object> trialSwitchRole(
            @RequestParam("role") String role,
            HttpSession session) {

        String sessionId = session.getId();
        log.info("역할 전환 - 세션: {}, 새 역할: {}", sessionId, role);

        Map<String, Object> response = new HashMap<>();

        try {
            // 역할 전환
            sessionManager.switchRole(sessionId, role);

            // 사건 정보 조회
            Case trialCase = sessionManager.getCase(sessionId);

            // 역할별 안내 메시지
            String roleNameKorean = getRoleNameKorean(role);

            String message;
            if (trialCase != null) {
                message = String.format("""
                    🔄 역할이 %s(으)로 전환되었습니다.
                    
                    현재 재판 중인 사건:
                    - 사건번호: %s
                    - 피고인: %s
                    - 혐의: %s
                    
                    %s의 관점에서 재판에 참여하실 수 있습니다.
                    """,
                        roleNameKorean,
                        trialCase.getCaseNumber(),
                        trialCase.getDefendant(),
                        trialCase.getCharge(),
                        roleNameKorean
                );
            } else {
                message = String.format("역할이 %s(으)로 전환되었습니다.", roleNameKorean);
            }

            response.put("success", true);
            response.put("newRole", role);
            response.put("roleNameKorean", roleNameKorean);
            response.put("message", message);
            response.put("caseInfo", trialCase);

        } catch (Exception e) {
            log.error("역할 전환 실패", e);
            response.put("success", false);
            response.put("message", "역할 전환 중 오류가 발생했습니다.");
        }

        return response;
    }

    /**
     * ⭐ 신규: 재판 종료 및 세션 초기화
     */
    @PostMapping("/trial-complete")
    public Map<String, Object> trialComplete(HttpSession session) {
        String sessionId = session.getId();
        log.info("재판 종료 - 세션: {}", sessionId);

        Map<String, Object> response = new HashMap<>();

        try {
            // 사건 정보 조회
            Case trialCase = sessionManager.getCase(sessionId);

            if (trialCase != null) {
                // 사건 상태를 'closed'로 업데이트
                trialCase.setStatus("closed");
                caseService.updateCase(trialCase);
            }

            // 세션 초기화
            sessionManager.clearSession(sessionId);

            response.put("success", true);
            response.put("message", "재판이 종료되었습니다.");
            response.put("caseNumber", trialCase != null ? trialCase.getCaseNumber() : null);

        } catch (Exception e) {
            log.error("재판 종료 실패", e);
            response.put("success", false);
            response.put("message", "재판 종료 중 오류가 발생했습니다.");
        }

        return response;
    }

    /**
     * ⭐ 신규: 현재 세션 정보 조회
     */
    @GetMapping("/trial-session-info")
    public Map<String, Object> getSessionInfo(HttpSession session) {
        String sessionId = session.getId();

        Map<String, Object> info = new HashMap<>();
        info.put("sessionId", sessionId);
        info.put("hasCase", sessionManager.hasCase(sessionId));
        info.put("currentRole", sessionManager.getCurrentRole(sessionId));

        Case trialCase = sessionManager.getCase(sessionId);
        if (trialCase != null) {
            info.put("caseNumber", trialCase.getCaseNumber());
            info.put("defendant", trialCase.getDefendant());
            info.put("charge", trialCase.getCharge());
        }

        return info;
    }

    // ===== Helper Methods =====

    private String getRoleNameKorean(String roleId) {
        return switch (roleId) {
            case "judge" -> "판사";
            case "prosecutor" -> "검사";
            case "defender" -> "변호인";
            case "defendant" -> "피고인";
            case "witness" -> "증인";
            case "jury" -> "참심위원";
            default -> "참가자";
        };
    }
}