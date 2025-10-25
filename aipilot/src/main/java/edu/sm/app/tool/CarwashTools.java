package edu.sm.app.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 실제 하드웨어 제어 영역은 여기로 집결.
 * 지금은 log만 찍고, 나중에 PLC/MQTT/장비 REST 호출로 대체 가능.
 */
@Component
@Slf4j
public class CarwashTools {

    @Tool(description = "입차 게이트(차단봉)를 연다.")
    public void gateOpen() {
        log.info("[TOOL] gateOpen() 차단봉 OPEN");
        // TODO: 실제 장비 호출
    }

    @Tool(description = "게이트(차단봉)를 닫는다.")
    public void gateClose() {
        log.info("[TOOL] gateClose() 차단봉 CLOSE");
        // TODO: 실제 장비 호출
    }

    @Tool(description = "세차 레시피를 실행한다. orderId와 레시피(JSON 문자열)를 받는다.")
    public String start(
            @ToolParam(description = "주문 ID", required = true) String orderId,
            @ToolParam(description = "레시피(JSON 문자열)", required = true) String recipeJson
    ) {
        log.info("[TOOL] start() orderId={}, recipe={}", orderId, recipeJson);
        // TODO: 실제 세차기 동작 시작
        return "running";
    }
}
