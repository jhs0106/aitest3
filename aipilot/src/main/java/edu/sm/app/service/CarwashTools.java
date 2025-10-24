package edu.sm.app.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 세차장 장비 제어용 @Tool 집합.
 * 지금은 목업으로 로그만 남기며, 나중에 PLC/브로커(REST/MQTT/Modbus) 호출로 교체.
 */
@Component
@Slf4j
public class CarwashTools {

    @Tool(description = "차단봉을 연다.")
    public void gateOpen() {
        // TODO: 실제 장비 제어 호출로 교체 (예: POST /tool/gate/open)
        log.info("[TOOL] gateOpen() 차단봉 OPEN");
    }

    @Tool(description = "차단봉을 닫는다.")
    public void gateClose() {
        // TODO: 실제 장비 제어 호출로 교체 (예: POST /tool/gate/close)
        log.info("[TOOL] gateClose() 차단봉 CLOSE");
    }

    @Tool(description = "세차 레시피를 실행한다.")
    public String start(
            @ToolParam(description = "주문 ID", required = true) String orderId,
            @ToolParam(description = "레시피(JSON 문자열)", required = true) String recipeJson
    ) {
        // TODO: recipeJson 파싱 → 단계별 노즐/압력/케미컬 설정을 장비 API로 순차 실행
        log.info("[TOOL] start() orderId={}, recipe={}", orderId, recipeJson);
        // 예: POST /tool/wash/start { orderId, recipe: {...} }
        return "running";
    }
}
