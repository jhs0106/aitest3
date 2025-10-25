package edu.sm.app.service;

import edu.sm.app.tool.CarwashTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 실제 장비 제어를 호출하는 중간 레이어.
 * 컨트롤러나 다른 서비스들이 직접 @Tool을 부르지 않고
 * 이 서비스를 거쳐서 호출할 수 있게 한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CarwashActuatorService {

    private final CarwashTools carwashTools;

    public void gateOpen() {
        if (carwashTools != null) {
            carwashTools.gateOpen();
        } else {
            log.info("[MOCK] gateOpen()");
        }
    }

    public void gateClose() {
        if (carwashTools != null) {
            carwashTools.gateClose();
        } else {
            log.info("[MOCK] gateClose()");
        }
    }

    public void startWash(String orderId, String recipeJson) {
        if (carwashTools != null) {
            carwashTools.start(orderId, recipeJson);
        } else {
            log.info("[MOCK] startWash(orderId={}, recipe={})", orderId, recipeJson);
        }
    }
}
