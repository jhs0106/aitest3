package edu.sm.app.service;

import edu.sm.app.dto.Case;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trial 세션 관리자
 * - 세션별 사건 정보 관리
 * - 세션별 현재 역할 추적
 * - 역할별 대화 맥락 분리를 위한 conversationId 생성
 */
@Component
@Slf4j
public class TrialSessionManager {

    // sessionId -> caseId 매핑
    private final Map<String, Integer> sessionCaseMap = new ConcurrentHashMap<>();

    // sessionId -> currentRole 매핑
    private final Map<String, String> sessionRoleMap = new ConcurrentHashMap<>();

    // sessionId -> Case 객체 캐시 (DB 조회 최소화)
    private final Map<String, Case> sessionCaseCache = new ConcurrentHashMap<>();

    /**
     * 세션 초기화 - 사건 선택 시 호출
     */
    public void initSession(String sessionId, Case trialCase) {
        log.info("세션 초기화 - sessionId: {}, 사건번호: {}", sessionId, trialCase.getCaseNumber());

        sessionCaseMap.put(sessionId, trialCase.getCaseId());
        sessionCaseCache.put(sessionId, trialCase);
        sessionRoleMap.put(sessionId, "defendant"); // 기본 역할: 피고인
    }

    /**
     * 역할 전환
     */
    public void switchRole(String sessionId, String newRole) {
        String oldRole = sessionRoleMap.get(sessionId);
        log.info("역할 전환 - sessionId: {}, {} -> {}", sessionId, oldRole, newRole);

        sessionRoleMap.put(sessionId, newRole);
    }

    /**
     * 현재 역할 조회
     */
    public String getCurrentRole(String sessionId) {
        return sessionRoleMap.getOrDefault(sessionId, "defendant");
    }

    /**
     * 사건 ID 조회
     */
    public Integer getCaseId(String sessionId) {
        return sessionCaseMap.get(sessionId);
    }

    /**
     * 사건 객체 조회
     */
    public Case getCase(String sessionId) {
        return sessionCaseCache.get(sessionId);
    }

    /**
     * 세션에 사건이 설정되어 있는지 확인
     */
    public boolean hasCase(String sessionId) {
        return sessionCaseCache.containsKey(sessionId);
    }

    /**
     * 역할별 conversationId 생성
     * 예: session123-defendant, session123-prosecutor
     * 이렇게 하면 역할별로 대화 맥락이 분리됨
     */
    public String buildConversationId(String sessionId, String roleId) {
        return sessionId + "-" + roleId;
    }

    /**
     * 현재 역할의 conversationId 생성
     */
    public String getCurrentConversationId(String sessionId) {
        String currentRole = getCurrentRole(sessionId);
        return buildConversationId(sessionId, currentRole);
    }

    /**
     * 세션 완전 초기화 (재판 종료 시)
     */
    public void clearSession(String sessionId) {
        log.info("세션 초기화 - sessionId: {}", sessionId);

        sessionCaseMap.remove(sessionId);
        sessionRoleMap.remove(sessionId);
        sessionCaseCache.remove(sessionId);
    }

    /**
     * 모든 세션 정보 조회 (디버깅용)
     */
    public Map<String, String> getAllSessionInfo() {
        Map<String, String> info = new ConcurrentHashMap<>();
        sessionCaseMap.forEach((sessionId, caseId) -> {
            String role = sessionRoleMap.get(sessionId);
            Case trialCase = sessionCaseCache.get(sessionId);
            info.put(sessionId, String.format("사건ID: %d, 역할: %s, 사건번호: %s",
                    caseId, role, trialCase != null ? trialCase.getCaseNumber() : "N/A"));
        });
        return info;
    }
}