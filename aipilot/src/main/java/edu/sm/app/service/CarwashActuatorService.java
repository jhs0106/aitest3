package edu.sm.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import org.springframework.beans.factory.annotation.Autowired;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarwashActuatorService {

    // 실제 구현 시 @Tool 메소드들을 모아놓은 빈(예: gateOpen/gateClose/start)
    @Autowired(required = false)
    private edu.sm.app.service.CarwashTools carwashTools;

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
//

    public void startWash(String orderId, String recipeJson) {
        if (carwashTools != null) {
            carwashTools.start(orderId, recipeJson);
        } else {
            log.info("[MOCK] startWash(orderId={}, recipe={})", orderId, recipeJson);
        }
    }
}
