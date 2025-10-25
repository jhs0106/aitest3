package edu.sm.controller;

import edu.sm.app.service.CarwashActuatorService;
import edu.sm.app.service.CarwashEntryService;
import edu.sm.app.service.CarwashFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * /ai6
 * - entry-image : 번호판 이미지 업로드 → plate 인식 → 게이트 제어 → RAG 준비
 * - plan-image  : 차량 전체 이미지 업로드 → 상태 분석 → 레시피 생성+실행
 * - exit-gate   : 출차 시 게이트 open/close
 */
@RestController
@RequestMapping("/ai6")
@RequiredArgsConstructor
@Slf4j
public class CarwashController {

    private final CarwashEntryService carwashEntryService;
    private final CarwashFlowService carwashFlowService;
    private final CarwashActuatorService carwashActuatorService;

    /**
     * 입차 단계.
     * 프런트: /springai3/carwash_entry.jsp 에서
     * FormData(attach) -> POST /ai6/entry-image
     */
    @PostMapping("/entry-image")
    public Map<String,Object> entryImage(@RequestParam("attach") MultipartFile attach) throws Exception {

        CarwashEntryService.EntryResult r = carwashEntryService.handleEntry(
                attach.getContentType(),
                attach.getBytes()
        );

        // r = { plate, knownCustomer, gate }
        return Map.of(
                "plate", r.getPlate(),
                "knownCustomer", r.isKnownCustomer(),
                "gate", r.getGate()
        );
    }

    /**
     * 레시피 생성 + 실행 단계.
     * 프런트: /springai3/carwash_plan.jsp 에서
     * URL ?plate=... & FormData(attach) -> POST /ai6/plan-image?plate=...
     */
    @PostMapping("/plan-image")
    public Map<String,Object> planImage(@RequestParam("plate") String plate,
                                        @RequestParam("attach") MultipartFile attach) throws Exception {

        CarwashFlowService.PlanAndExecuteResult r = carwashFlowService.planAndExecute(
                plate,
                attach.getContentType(),
                attach.getBytes()
        );

        return Map.of(
                "orderId", r.getOrderId(),
                "recipeJson", r.getRecipeJson(),
                "status", r.getStatus()
        );
    }

    /**
     * 출차/차단봉 제어.
     * 프런트: /springai3/carwash_progress.jsp 에서
     *   POST /ai6/exit-gate?plate=12가3456&action=open
     *   POST /ai6/exit-gate?plate=12가3456&action=close
     *
     * 여기서는 plate는 로깅이나 감사 용도로만 사용.
     */
    @PostMapping("/exit-gate")
    public Map<String,Object> exitGate(@RequestParam("plate") String plate,
                                       @RequestParam("action") String action) {
        if ("open".equalsIgnoreCase(action)) {
            carwashActuatorService.gateOpen();
            return Map.of("plate", plate, "gate", "OPENED");
        } else if ("close".equalsIgnoreCase(action)) {
            carwashActuatorService.gateClose();
            return Map.of("plate", plate, "gate", "CLOSED");
        } else {
            return Map.of("plate", plate, "gate", "UNKNOWN_ACTION");
        }
    }
}
