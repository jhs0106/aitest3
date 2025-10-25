package edu.sm.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarwashFlowService {

    private final ChatModel chatModel;
    private final CarwashPlanService carwashPlanService;
    private final CarwashActuatorService carwashActuatorService;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 세차 계획 수립 + 장비 시작 + DB 기록
     * 1) 차량 상태 분석 (이미지 기반)
     * 2) 한글 세차 레시피(JSON) 생성 (RAG + 차량 히스토리)
     * 3) wash_order / wash_log / vehicle 업데이트
     * 4) 하드웨어 시작 시그널
     */
    public PlanAndExecuteResult planAndExecute(String plate, String contentType, byte[] bytes) throws Exception {

        // 1) 이미지로 차량 상태 추출 (색상/크기/오염도 등)
        Map<String,Object> visionInfo = analyzeCarImage(contentType, bytes);
        // 예: { soilLevel:"heavy", color:"검정", size:"중형" }

        // 2) LLM + RAG로 맞춤형 세차 레시피(한국어 JSON) 생성
        String recipeJson = carwashPlanService.generateRecipeJsonSync(
                plate,
                Map.of("vision", visionInfo)
        );

        // 파싱해서 가격/ETA 꺼내기
        JsonNode recipeNode = objectMapper.readTree(recipeJson);
        int price   = recipeNode.path("price").asInt(0);
        int etaMin  = recipeNode.path("etaMin").asInt(0);

        // 3) 세차 주문 ID 생성
        String orderId = "W-" + UUID.randomUUID();

        // 4) wash_order INSERT
        jdbc.update("""
            INSERT INTO wash_order (id, plate, recipe_json, status, price, eta_min, created_at)
            VALUES (?,?,?,?,?,?,NOW())
        """, orderId, plate, recipeJson, "RUNNING", price, etaMin);

        // 5) wash_log INSERT (각 단계별 초기 로그)
        //    recipe 배열은 2가지 케이스를 허용:
        //    (A) ["1단계: ...", "2단계: ...", ...]  <-- 문자열
        //    (B) [{"step":"preRinse","pressureBar":70,...}, {...}] <-- 객체
        JsonNode steps = recipeNode.path("recipe");
        if (steps.isArray()) {
            int idx = 0;
            for (JsonNode stepNode : steps) {

                String stepName;
                Integer pressureBar = null;
                String chemCode = null;

                if (stepNode.isTextual()) {
                    // 케이스 A: 한글 설명 문자열 전체를 stepName으로 저장
                    stepName = stepNode.asText(); // 예: "1단계: 차량 전체에 물을 뿌려 ..."

                } else {
                    // 케이스 B: 객체 기반
                    // step 필드가 있으면 우선 사용, 없으면 fallback
                    stepName = stepNode.path("step").asText("STEP-" + idx);

                    if (stepNode.has("pressureBar")) {
                        pressureBar = stepNode.get("pressureBar").asInt();
                    }
                    if (stepNode.has("chem")) {
                        chemCode = stepNode.get("chem").asText();
                    } else if (stepNode.has("chem_code")) {
                        chemCode = stepNode.get("chem_code").asText();
                    }
                }

                jdbc.update("""
                    INSERT INTO wash_log
                      (order_id, step_idx, step_name, started_at, ended_at,
                       pressure_bar, chem_code, result)
                    VALUES ( ?, ?, ?, NOW(), NULL, ?, ?, ? )
                """,
                        orderId,
                        idx,
                        stepName,
                        pressureBar,
                        chemCode,
                        "PENDING" // 아직 완료 안 됨
                );

                idx++;
            }
        }

        // 6) vehicle 테이블 업데이트
        //    방금 찍은 이미지에서 알아낸 color/size와 세차 timestamp 반영
        jdbc.update("""
            UPDATE vehicle
            SET color = COALESCE(?, color),
                size = COALESCE(?, size),
                last_wash_at = ?
            WHERE plate = ?
        """,
                (String) visionInfo.get("color"),
                (String) visionInfo.get("size"),
                Timestamp.from(Instant.now()),
                plate
        );

        // 7) 실제 장비에 startWash 시그널
        //    (실제 제어는 CarwashActuatorService에서 추상화)
        carwashActuatorService.startWash(orderId, recipeJson);

        // 8) 프론트에 내려줄 결과
        PlanAndExecuteResult result = new PlanAndExecuteResult();
        result.setOrderId(orderId);
        result.setRecipeJson(recipeJson);
        result.setStatus("RUNNING");
        result.setPrice(price);
        result.setEtaMin(etaMin);
        return result;
    }

    /**
     * 차의 실사 사진(오염, 색상 등)을 LLM 비전으로 분석해서
     * 한글/사람 친화적 정보로 추출한다.
     *
     * 주의: 반환은 Map<String,Object> 형태이고
     *  - soilLevel: "light"/"medium"/"heavy" 그대로 둘게 (엔지니어용)
     *  - color: "검정", "흰색", "은색", "파란색" 같이 한글
     *  - size:  "소형", "중형", "SUV" 같이 한글
     *
     * 이 정보는 이후 RAG 컨텍스트로 레시피 생성에 들어간다.
     */
    private Map<String,Object> analyzeCarImage(String contentType, byte[] bytes) throws Exception {
        Media media = Media.builder()
                .mimeType(MimeType.valueOf(contentType))
                .data(new ByteArrayResource(bytes))
                .build();

        UserMessage userMessage = UserMessage.builder()
                .text("""
                    이 차량 이미지를 보고 다음 정보를 분석해서 JSON으로만 반환하세요.
                    다른 말은 하지 마세요. JSON 외 출력 금지.

                    필드는 다음과 같습니다.
                    - soilLevel : 오염도. "light", "medium", "heavy" 중 하나로만 적으세요.
                                  (예: "heavy" = 매우 더럽다)
                    - color     : 차량의 대표 색상을 한국어 한 단어로 적으세요.
                                  예: "검정", "흰색", "은색", "빨간색", "파란색"
                    - size      : 차량의 크기를 한국어로 적으세요.
                                  "소형", "중형", "SUV" 중 하나만 사용하세요.

                    예시 출력:
                    {
                      "soilLevel": "heavy",
                      "color": "검정",
                      "size": "중형"
                    }

                    위 예시와 동일하게 JSON 형식으로만 답하세요.
                """)
                .media(media)
                .build();

        ChatClient chatClient = ChatClient.builder(chatModel).build();
        String llmResponse = chatClient
                .prompt()
                .messages(userMessage)
                .call()
                .content();

        log.info("[PLAN-IMAGE] analyzeCarImage raw={}", llmResponse);

        // 혹시 코드블럭 마크다운 감싸오면 제거
        String cleaned = llmResponse
                .replace("```json", "")
                .replace("```", "")
                .trim();

        JsonNode node = objectMapper.readTree(cleaned);

        Map<String,Object> vision = new HashMap<>();
        vision.put("soilLevel", node.path("soilLevel").asText("medium"));
        vision.put("color",     node.path("color").asText("검정"));   // 한글 기본값
        vision.put("size",      node.path("size").asText("중형"));   // 한글 기본값

        return vision;
    }

    @lombok.Data
    public static class PlanAndExecuteResult {
        private String orderId;
        private String recipeJson;
        private String status;
        private int price;
        private int etaMin;
    }
}
