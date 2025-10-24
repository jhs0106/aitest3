package edu.sm.app.service;

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

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarwashPlanService {

    private final JdbcTemplate jdbc;
    private final VectorStore vectorStore;
    private final ChatClient.Builder chatClientBuilder;

    /**
     * DB의 차량/코팅/선호 정보를 읽어와 벡터스토어에 "차량 프로필" 도큐먼트로 적재
     * - 이미 존재해도 중복되어도 무해(최근 임베딩으로 덮어쓰기 정책을 원하면 별도 upsert 로직 추가)
     * @param plate 차량 번호판(예: "12가3456")
     * @return 기존 고객 여부(true면 vehicle row 존재)
     */
    public boolean ensureVehicleProfileEmbedding(String plate) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
            select v.plate, v.model, v.size, v.color, v.last_wash_at,
                   (select coat_type   from coat_history ch where ch.plate=v.plate order by coated_at desc limit 1) as coat_type,
                   (select coated_at   from coat_history ch where ch.plate=v.plate order by coated_at desc limit 1) as coated_at,
                   coalesce((select want_foam         from pref p where p.plate=v.plate limit 1), false) as want_foam,
                   coalesce((select want_wheel_detail from pref p where p.plate=v.plate limit 1), false) as want_wheel_detail,
                   coalesce((select no_alkaline       from pref p where p.plate=v.plate limit 1), false) as no_alkaline,
                   coalesce((select max_pressure      from pref p where p.plate=v.plate limit 1), 120)  as max_pressure
            from vehicle v
            where v.plate = ?
        """, plate);

        if (rows.isEmpty()) {
            log.info("vehicle not found for plate={} (신규 고객)", plate);
            return false;
        }

        Map<String, Object> r = rows.get(0);
        String content = """
            차량 프로필
            번호판: %s
            모델: %s
            크기: %s
            색상: %s
            최근세차: %s
            최근 코팅: %s (%s)
            선호: foam=%s, wheelDetail=%s, noAlkaline=%s
            maxPressure: %s
            안전규칙 메모:
            - 검정/세라믹 코팅 차량은 pH 9 초과 금지
            - 압력은 maxPressure 이하 유지
            """.formatted(
                r.get("plate"), r.get("model"), r.get("size"), r.get("color"),
                r.get("last_wash_at"),
                r.get("coat_type"), r.get("coated_at"),
                r.get("want_foam"), r.get("want_wheel_detail"), r.get("no_alkaline"),
                r.get("max_pressure")
        );

        Document doc = new Document(content);
        // 검색 필터에 사용할 메타데이터들
        doc.getMetadata().put("type", "carwash");
        doc.getMetadata().put("source", "vehicle_profile");
        doc.getMetadata().put("plate", String.valueOf(r.get("plate")));
        doc.getMetadata().put("model", String.valueOf(r.get("model")));
        doc.getMetadata().put("size", String.valueOf(r.get("size")));
        doc.getMetadata().put("color", String.valueOf(r.get("color")));
        doc.getMetadata().put("coat_type", String.valueOf(r.get("coat_type")));
        doc.getMetadata().put("max_pressure", String.valueOf(r.get("max_pressure")));

        vectorStore.add(List.of(doc));
        log.info("vehicle profile embedded: plate={}", plate);
        return true;
    }

    /**
     * plate와 컨텍스트(vision/weather 등)를 받아 LLM이 레시피(JSON만)를 스트리밍으로 생성
     * @param plate  차량 번호판
     * @param ctx    {"vision":{"soilLevel":"medium"}, "weather":{"pm10":60,"rainProb":10}} 등
     * @return Flux<String> (text/plain 스트림)
     */
    public Flux<String> plan(String plate, Map<String, Object> ctx) {
        // 1) VectorStore 검색 요청(plate + type=carwash로 강하게 필터)
        SearchRequest sr = SearchRequest.builder()
                .similarityThreshold(0.0)
                .topK(4)
                .filterExpression("type == 'carwash' && plate == '" + escapeQuotes(plate) + "'")
                .build();

        QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(sr)
                .build();

        // 2) ChatClient 준비(로그 어드바이저 포함)
        ChatClient chatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1))
                .build();

        String userPayload = toJson(Map.of(
                "plate", plate,
                "context", ctx
        ));

        // 3) LLM 호출: 안전규칙 + JSON 스키마 강제
        return chatClient.prompt()
                .system("""
                너는 차량 세차 레시피 플래너다.
                안전규정:
                - 압력은 차량 프로필의 maxPressure 이하여야 한다.
                - 검정차 또는 세라믹 코팅 차량은 pH 9 초과 금지.
                출력형식:
                - 항상 JSON만 반환한다. (설명문 X)
                - 키: recipe[], safetyNotes[], price, etaMin
                예시:
                {"recipe":[{"step":"preRinse","nozzle":"wide","pressureBar":70,"chem":"none","durationSec":60}],
                 "safetyNotes":["avoid pH > 9"],"price":18000,"etaMin":9}
                """)
                .user(userPayload)
                .advisors(advisor)
                .stream()
                .content();
    }

    // -------------------
    // private helpers
    // -------------------
    private String toJson(Object o) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(o);
        } catch (Exception e) {
            log.warn("toJson error: {}", e.getMessage());
            return "{}";
        }
    }

    private String escapeQuotes(String s) {
        return s == null ? "" : s.replace("'", "''");
    }
}
