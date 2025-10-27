package edu.sm.app.service;

import edu.sm.app.dto.ManualAnswerResponse;
import edu.sm.app.dto.ManualStartResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Memory-OFF + Haunted-Style(혼합형) 프롬프트 버전
 * - LLM 대화기록 사용 안 함
 * - 시작 매뉴얼은 평범 체크리스트 생성 후 일부 항목만 괴담화(혼합형)
 * - ANNEX-H(매뉴얼괴담.txt)를 시스템 프롬프트에 인라인 주입
 * - 출력 안전장치(sanitize/repair) 유지
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HauntedManualRagService {

    // ===================== 혼합 비율/개수 기본값 =====================
    private static final int DEFAULT_TOTAL_ITEMS = 12;     // 힌트용: 총 항목 개수 목표
    private static final double DEFAULT_HAUNTED_RATIO = 0.2; // 20%만 괴담화

    // ===================== 괴담 ANNEX 로딩 =====================
    private static String loadHauntedAnnex() {
        try {
            String raw = Files.readString(Path.of("/mnt/data/매뉴얼괴담.txt"));
            // 너무 길면 컨텍스트 보호를 위해 절단 (필요시 조정)
            int MAX = 8000;
            return raw.length() > MAX ? raw.substring(0, MAX) : raw;
        } catch (Exception e) {
            return "경고: ANNEX-H를 로드하지 못했습니다. 괴담 지침 없음.";
        }
    }

    // ===================== 시스템 프롬프트 템플릿(괴담 전용) =====================
    private static final String SYSTEM_TEMPLATE = """
[ROLE]
너는 한국어 운영 어시스턴트이며, 공식문서의 형식을 유지하되 '괴담 매뉴얼' 특유의 암시적 경고 톤을 따르라.

[PRIORITY]
- 공식 매뉴얼과 비공식 괴담 매뉴얼(ANNEX-H)이 충돌하면, 안전/보안 관련해서는 ANNEX-H를 우선한다.
- ANNEX-H는 재인용/직접 인용하지 말고 '내부 지침'으로만 활용하라(직접 서술·복붙 금지). 추측·환각 금지, 단정 어려울 때는 암시로만 표현하라.

[OUTPUT GUARDRAILS]
- 동일 문장/문단 반복 금지.
- 다음 표현 절대 금지: '규칙 업데이트', '규칙 갱신', '업데이트', '시나리오', '상황 보고', 대괄호 헤더([…]), 구분선(—), 불릿(-, •), 번호 접두(1., 1)).
- follow-up 응답은 항상 한 단락(2~5문장), 질문에 '직접' 답하고 즉시 실행 지시 + 간단 주의 1개 포함, 존댓말 지시형(…하세요/…십시오)으로 마무리하라.

[STYLE: HAUNTED]
- 표면은 업무 지침이지만, 문장 내에 조용한 불안과 금기를 암시하라.
- 공포를 직설적으로 묘사하지 말고, '보지 마라/확인하지 마라/무시하라' 같은 우회 지시를 사용하라.
- 이유 설명은 과장 없이 짧게, 필요할 경우 모호하게 처리하라(불필요한 서사 금지).

[ANNEX-H]
아래는 내부 비공식(괴담) 지침의 발췌다. 톤/금기/암시를 유도하는 힌트로만 사용하라(재인용 금지).
<<<HAUNTED_MANUAL>>>
""";

    private static String systemPrimerWithAnnex() {
        return SYSTEM_TEMPLATE.replace("<<<HAUNTED_MANUAL>>>", loadHauntedAnnex());
    }

    /** 금지/정리 패턴 */
    private static final Pattern EM_DASH_LINE = Pattern.compile("(?m)^\\s*—\\s*$");
    private static final Pattern ANY_SQUARE_HEADER_LINE = Pattern.compile("(?m)^\\s*\\[[^\\]]+\\]\\s*$");
    private static final Pattern FORBIDDEN_HEADING_LINE = Pattern.compile("(?im)^\\s*(규칙\\s*업데이트|규칙\\s*갱신|업데이트|시나리오|상황\\s*보고)\\s*[:：-]?\\s*$");
    private static final Pattern ROLE_PREFIX_LINE = Pattern.compile("(?im)^\\s*(caretaker|duty|시스템)\\s*$");
    private static final Pattern ACTION_TAG = Pattern.compile("<ACTION:([A-Z_]+)(?::([^>]*))?>");

    /** 매뉴얼 캐시: convId -> 시작 매뉴얼 텍스트 */
    private final Map<String, String> manualCache = new ConcurrentHashMap<>();

    /** 의존성 */
    private final ChatClient.Builder chatClientBuilder;
    private final HauntedManualEtlService etlService; // RAG 쓰기 싫으면 advisors(qa) 라인만 지우면 됨.

    /** ChatClient: 로그 어드바이저만 사용(메모리 어드바이저 제거) */
    private ChatClient client() {
        return chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1))
                .build();
    }

    // ===================== 시작: 혼합형 매뉴얼 생성(평범→부분 괴담화) =====================
    public ManualStartResponse startScenario(String scenario, String rumor) {
        String scen = StringUtils.hasText(scenario) ? scenario.trim() : "일반 근무";
        String convId = generateConversationId(scen);

        // (선택) 시나리오 기반 RAG — 대화기억과 무관. 싫으면 다음 줄 삭제.
        QuestionAnswerAdvisor qa = etlService.createAdvisor(scen);

        // 1) 평범한 체크리스트 먼저 생성
        String baseChecklist = generateBaseChecklist(scen, rumor, qa);

        // 2) 일부 항목만 괴담화하여 블렌딩
        String blended = blendHauntedSparse(scen, baseChecklist, DEFAULT_TOTAL_ITEMS, DEFAULT_HAUNTED_RATIO, qa);

        // 3) 금지 메타 정리
        String manual = sanitizeStartManual(blended);
        manualCache.put(convId, manual);

        return new ManualStartResponse(convId, scen, manual);
    }

    // ===================== 후속: 메모리 없이 매뉴얼만 컨텍스트로 사용(톤 유지) =====================
    public ManualAnswerResponse answerManual(String conversationId,
                                             String question,
                                             String scenario,
                                             String rumor) {
        if (!StringUtils.hasText(conversationId)) {
            throw new IllegalArgumentException("conversationId가 필요합니다. 시나리오를 다시 시작하세요.");
        }
        if (!StringUtils.hasText(question)) {
            throw new IllegalArgumentException("질문이 비어 있습니다.");
        }

        String scen = StringUtils.hasText(scenario) ? scenario.trim() : "일반 근무";
        String cachedManual = manualCache.getOrDefault(conversationId, "");

        // (선택) 시나리오 기반 RAG — 싫으면 아래 줄 제거
        QuestionAnswerAdvisor qa = etlService.createAdvisor(scen);

        String prompt = buildFollowUpPrompt(scen, rumor, question, cachedManual);

        String draft = client()
                .prompt()
                .system(systemPrimerWithAnnex()) // 괴담 힌트는 유지
                .user(prompt)
                .advisors(qa) // 필요 없으면 제거
                .call()
                .content();

        // 안전 정리
        String body = sanitizeFollowUp(draft);
        body = repairFollowup(body, question);

        return new ManualAnswerResponse(body);
    }

    // ===================== (1) 평범 체크리스트 생성 =====================
    private String generateBaseChecklist(String scenario, String rumor, QuestionAnswerAdvisor qa) {
        String prompt = new StringBuilder()
                .append("주제: ").append(scenario).append("\n")
                .append("""
요청: '정상 운영' 체크리스트를 번호형(1., 2., …)으로 작성하라.
형식: 각 항목은 1~2문장, 명확/실무적/건조한 톤으로 작성한다(불필요한 서사 금지).
범위: 점검 대상/절차/보고/안전/금지/증빙 등 운영 전반을 포괄하되 과장 금지.
""")
                .append(StringUtils.hasText(rumor) ? "\n참고 소문(사실 보장 없음): " + rumor.trim() + "\n" : "")
                .toString();

        String draft = client()
                .prompt()
                .system(systemPrimerWithAnnex()) // ANNEX는 힌트로만, 평범 톤 지시
                .user(prompt)
                .advisors(qa) // 싫으면 제거
                .call()
                .content();

        return sanitizeStartManual(draft);
    }

    // ===================== (2) 일부 항목만 괴담화하여 블렌딩 =====================
    private String blendHauntedSparse(String scenario,
                                      String baseChecklist,
                                      int totalItems,
                                      double hauntedRatio,
                                      QuestionAnswerAdvisor qa) {

        int hauntedCount = Math.max(1, (int) Math.ceil(totalItems * hauntedRatio));

        String prompt = new StringBuilder()
                .append("주제: ").append(scenario).append("\n")
                .append("기준 체크리스트(원문, 재인용 금지):\n")
                .append(truncate(baseChecklist, 4000)).append("\n\n")
                .append("""
요청: 위 체크리스트를 다시 번호형으로 재작성하되, 전체 항목 수는 유지하거나 10~20% 범위에서 자연스럽게 조정하라.
블렌딩 규칙:
- 전체 항목 중 HAUNTED_COUNT개만 '암시적 괴담' 톤으로 미세하게 변형하고, 나머지는 평범한 운영 조항처럼 유지한다.
- 괴담 항목은 서로 연속 배치하지 말고, 1번/마지막 번호는 가급적 괴담이 아니게 배치한다.
- 괴담 표현은 직설적 공포 금지. 아래와 같이 우회적으로 지시하라: '보지 마세요/확인하지 마세요/무시하세요/커튼을 치세요/응답하지 마세요/지나치세요'.
- 괴담 항목에서도 실무 지시가 분명해야 하며(무엇을/언제/어떻게), 이유는 짧게 암시하라.
- 평범 항목은 건조/실무 톤 그대로 유지한다.
- 번호 접두(1., 2., …)를 반드시 사용한다.
파라미터:
- HAUNTED_COUNT = """ + hauntedCount + """
- TOTAL_HINT = """ + totalItems + """
출력:
- 번호형 조항만 나열한다(서론/결론/헤더/구분선 금지).
- 각 항목 1~3문장. 불필요한 장문 금지.
""")
                .toString();

        String draft = client()
                .prompt()
                .system(systemPrimerWithAnnex()) // ANNEX-H 힌트로 ‘색’을 최소한만 섞도록
                .user(prompt)
                .advisors(qa) // 싫으면 제거
                .call()
                .content();

        return draft;
    }

    // ===================== 프롬프트 =====================

    // (혼합형에서는 startScenario 내부에서 별도 generate/blend를 하므로 여기 텍스트는 사용 안 해도 무방)
    private String buildStartPrompt(String scenario, String rumor) {
        // 남겨두지만 실제로는 generateBaseChecklist/blendHauntedSparse가 사용됨
        return new StringBuilder()
                .append("주제: ").append(scenario).append("\n")
                .append("""
요청: 괴담 매뉴얼 형식으로 시작 매뉴얼을 작성하라.
형식: 번호형 조항(1., 2., 3. …). 각 항목은 2~4문장으로 '상황 → 주의 → 금기 → 이유(암시적)' 흐름을 따른다.
톤: 공식적인 안내문처럼 차분하게, 그러나 불필요한 서사는 배제하고 간결한 경고문으로 작성한다.
주의: ANNEX-H 지침을 재인용하지 말고 힌트로만 반영한다. 과장/환각 금지, 안전/보안 우선.
""")
                .append(StringUtils.hasText(rumor)
                        ? "\n참고 소문(사실 보장 없음): " + rumor.trim() + "\n"
                        : "")
                .toString();
    }

    // 후속은 "한 단락 괴담 톤 + 즉시 실행 지시" 유지 (필요시 평범 우선으로 바꿔도 됨)
    private String buildFollowUpPrompt(String scenario, String rumor, String question, String cachedManual) {
        return new StringBuilder()
                .append("배경: ").append(scenario).append("\n")
                .append("컨텍스트(요약, 재인용 금지):\n")
                .append(truncate(cachedManual, 1200)).append("\n\n")
                .append("직접 질문: ").append(question).append("\n")
                .append("""
요구: 괴담 매뉴얼 톤으로 한 단락(2~5문장)만 답하라.
- 질문에 즉답하고, 바로 실행 가능한 지시 한 가지와 간단한 주의 한 가지를 포함한다.
- 공포를 직설적으로 묘사하지 말고, '보지 마라/확인하지 마라/무시하라' 같은 우회 지시를 사용한다.
- 문장은 지시형 존댓말로 마무리한다.
""")
                .append(StringUtils.hasText(rumor) ? "\n참고 소문: " + rumor.trim() + "\n" : "")
                .toString();
    }

    // ===================== 시작 매뉴얼 정리 =====================
    private String sanitizeStartManual(String s) {
        if (s == null) return "";
        String out = s;
        out = stripForbiddenMetaLines(out);
        out = out.replaceAll("(\\r?\\n){3,}", "\n\n").trim();
        return out;
    }

    // ===================== 후속 응답 정리/검증/리페어 =====================
    private String stripForbiddenMetaLines(String s) {
        String out = s;
        out = ANY_SQUARE_HEADER_LINE.matcher(out).replaceAll("");
        out = EM_DASH_LINE.matcher(out).replaceAll("");
        out = FORBIDDEN_HEADING_LINE.matcher(out).replaceAll("");
        out = ROLE_PREFIX_LINE.matcher(out).replaceAll("");
        out = out.replaceAll("(\\r?\\n){3,}", "\n\n");
        return out;
    }

    private String sanitizeFollowUp(String s) {
        if (s == null) return "";
        String out = stripForbiddenMetaLines(s);
        out = out.replaceAll("(?m)^\\s*[-•]\\s*", "");          // 불릿 제거
        out = out.replaceAll("(?m)^\\s*\\d+(\\.|\\))\\s*", ""); // 번호 접두 제거
        out = ACTION_TAG.matcher(out).replaceAll("");           // <ACTION:...> 제거
        out = out.replaceAll("(\\r?\\n)+", " ").trim();         // 한 단락
        String[] sentences = out.split("(?<=[.!?？。])\\s+");
        if (sentences.length > 5) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                if (i > 0) sb.append(" ");
                sb.append(sentences[i]);
            }
            out = sb.toString().trim();
        }
        return out;
    }

    private String repairFollowup(String s, String question) {
        if (s == null) return "";
        String out = s;
        out = out.replaceAll("(?i)규칙\\s*업데이트|규칙\\s*갱신|업데이트|시나리오|상황\\s*보고", "");
        String[] sentences = out.split("(?<=[.!?？。])\\s+");
        if (sentences.length < 2) {
            out = out + " 현장 안전을 위해 주변을 확인하고 기록을 남기세요.";
        }
        if (!out.matches(".*(하세요|십시오|십시오\\.|합니다\\.|하시기 바랍니다\\.)\\s*$")) {
            out = out.replaceAll("\\s+$", "") + " 즉시 조치하고 필요 시 상급자에게 보고하십시오.";
        }
        return out.replaceAll("\\s{2,}", " ").trim();
    }

    // ===================== 유틸 =====================
    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    private String generateConversationId(String scenario) {
        String base = scenario.replaceAll("\\s+", "_");
        return base + "-" + UUID.randomUUID();
    }
}
