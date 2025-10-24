package edu.sm.controller;

import edu.sm.app.service.CarwashActuatorService;
import edu.sm.app.service.CarwashPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**

 */
@RestController
@RequestMapping("/ai6")
@RequiredArgsConstructor
@Slf4j
public class CarwashController {

    private final CarwashPlanService carwashPlanService;
    private final CarwashActuatorService carwashActuatorService;


     // 차단봉 열기?

    @PostMapping("/entry-detect")
    public Map<String, Object> entryDetect(@RequestParam String plate) {
        boolean known = carwashPlanService.ensureVehicleProfileEmbedding(plate);
        carwashActuatorService.gateOpen();
        return Map.of(
                "plate", plate,
                "knownCustomer", known,
                "gate", "OPENED"
        );
    }

//    조합 만들기
    @PostMapping(value = "/plan", produces = "text/plain")
    public Flux<String> plan(@RequestParam String plate,
                             @RequestBody Map<String, Object> ctx) {
        return carwashPlanService.plan(plate, ctx);
    }

// 세차 진행
    @PostMapping("/execute")
    public String execute(@RequestParam String orderId,
                          @RequestBody String recipeJson) {
        carwashActuatorService.startWash(orderId, recipeJson);
        return "RUNNING";
    }
}
