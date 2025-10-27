package edu.sm.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarwashPlanService {

    private final JdbcTemplate jdbc;
    private final VectorStore vectorStore;
    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * plate 기반으로 vehicle row를 확보하고(없으면 INSERT), 그 데이터를 vector_store로 넣는다.
     * return: true면 기존에 있던 고객(이미 있었던 row),
     *         false면 이번에 새로 만든 row(신규 방문 차량)
     */
    public boolean ensureVehicleProfileEmbedding(String plate) {
        // 1) row 조회
        List<Map<String, Object>> rows = jdbc.queryForList("""
            select plate, customer_id, model, size, color, last_wash_at
            from vehicle
            where plate = ?
        """, plate);

        boolean existedBefore = !rows.isEmpty();

        if (!existedBefore) {
            // 아직 우리 DB에 없는 차량이면 기본값으로 등록
            jdbc.update("""
                insert into vehicle(plate, customer_id, model, size, color, last_wash_at)
                values (?, NULL, NULL, NULL, NULL, NULL)
            """, plate);
            log.info("[vehicle] 신규 plate 등록: {}", plate);

            // 다시 읽는다
            rows = jdbc.queryForList("""
                select plate, customer_id, model, size, color, last_wash_at
                from vehicle
                where plate = ?
            """, plate);
        }

        Map<String,Object> r = rows.get(0);

        // 2) vector_store 삽입용 문서 구성
        String content = """
            차량 프로필
            번호판: %s
            고객ID: %s
            모델: %s
            크기: %s
            색상: %s
            최근세차: %s
            안전규칙 메모:
            - 검정/세라믹 코팅 차량은 pH 9 초과 금지
            - 압력은 차량 제한 이하 유지
            """.formatted(
                r.get("plate"),
                r.get("customer_id"),
                r.get("model"),
                r.get("size"),
                r.get("color"),
                r.get("last_wash_at")
        );

        Document doc = new Document(content);
        // 메타데이터로 검색필터 지정
        doc.getMetadata().put("type", "carwash");
        doc.getMetadata().put("plate", String.valueOf(r.get("plate")));
        doc.getMetadata().put("model", String.valueOf(r.get("model")));
        doc.getMetadata().put("size", String.valueOf(r.get("size")));
        doc.getMetadata().put("color", String.valueOf(r.get("color")));
        doc.getMetadata().put("last_wash_at", String.valueOf(r.get("last_wash_at")));

        vectorStore.add(List.of(doc));
        log.info("vector_store에 차량 프로필 저장: {}", plate);

        return existedBefore;
    }

    // ===== 레시피 생성 부분은 그대로 유지 =====

    public Flux<String> plan(String plate, Map<String,Object> ctx) {
        QuestionAnswerAdvisor advisor = buildAdvisorForPlate(plate);
        ChatClient chatClient = buildChatClient();

        String userPayload = toJson(Map.of(
                "plate", plate,
                "context", ctx
        ));

        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userPayload)
                .advisors(advisor)
                .stream()
                .content();
    }

    public String generateRecipeJsonSync(String plate, Map<String,Object> ctx) {
        QuestionAnswerAdvisor advisor = buildAdvisorForPlate(plate);
        ChatClient chatClient = buildChatClient();

        String userPayload = toJson(Map.of(
                "plate", plate,
                "context", ctx
        ));

        String recipeJson = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userPayload)
                .advisors(advisor)
                .call()
                .content();

        recipeJson = sanitizeJson(recipeJson);
        return recipeJson;
    }

    private QuestionAnswerAdvisor buildAdvisorForPlate(String plate) {
        SearchRequest sr = SearchRequest.builder()
                .similarityThreshold(0.0)
                .topK(4)
                .filterExpression("type == 'carwash' && plate == '" + escapeQuotes(plate) + "'")
                .build();

        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(sr)
                .build();
    }

    private ChatClient buildChatClient() {
        return chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1))
                .build();
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            log.warn("toJson error: {}", e.getMessage());
            return "{}";
        }
    }

    private String escapeQuotes(String s) {
        return s == null ? "" : s.replace("'", "''");
    }

    private String sanitizeJson(String s) {
        if(s == null) return "";
        return s.replace("```json","")
                .replace("```","")
                .trim();
    }

    private static final String SYSTEM_PROMPT = """
        너는 '자동차 셀프 세차 가이드 생성기' 역할을 맡는다.

        아래 규칙을 반드시 지켜서 "JSON 하나만" 출력한다.
        다른 문장, 해설, 마크다운, ``` 코드블록은 절대 넣지 마라.
        오직 JSON 오브젝트 한 개만 반환해라.

        [JSON 스키마]
        {
          "recipe": [
             "...한국어 단계 설명 1...",
             "...한국어 단계 설명 2...",
             "...한국어 단계 설명 3...",
             ...
          ],
          "safetyNotes": [
             "...한국어 안전/주의 문장 1...",
             "...한국어 안전/주의 문장 2..."
          ],
          "price": 30000,
          "etaMin": 45
        }

        세부 규칙:
        1. "recipe" 배열
           - 각 원소는 한 단계의 작업 절차를 한국어로 자세하고 자연스럽게 설명한다.
           - 예) "차량 전체에 물을 충분히 뿌려 큰 먼지를 먼저 제거합니다."
           - 절대 영어로 설명하지 말 것.

        2. "safetyNotes" 배열
           - 세차 시 지켜야 할 주의/안전 수칙을 한국어로 작성한다.
           - 예) "검정 차량은 고알칼리 세제를 사용하지 마세요. 도장면 손상 위험이 있습니다."
           - 절대 영어로 설명하지 말 것.

        3. "price"
           - 세차 예상 금액(원). 정수만 넣어라. 예: 30000

        4. "etaMin"
           - 예상 소요 시간(분). 정수만 넣어라. 예: 45

        5. 키 이름(recipe, safetyNotes, price, etaMin)은 반드시 저 형태의 영문 그대로 사용해라.
           값(문장)만 한국어로 작성하라.

        6. safetyNotes에도 영어 금지. (압력, 세제 이름 등도 한국어로 표현하라.)
        """;
}
