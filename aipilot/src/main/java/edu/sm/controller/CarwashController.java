package edu.sm.controller;

import edu.sm.app.service.CarwashEntryService;
import edu.sm.app.service.CarwashEntryService.EntryResult;
import edu.sm.app.service.CarwashEntryService.ManualOpenResult;
import edu.sm.app.service.CarwashFlowService;
import edu.sm.app.service.CarwashFlowService.PlanAndExecuteResult;
import edu.sm.app.service.ExitGateService;
import edu.sm.app.service.ExitGateService.GateActionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ai6")
@RequiredArgsConstructor
@Slf4j
public class CarwashController {

    private final CarwashEntryService carwashEntryService;
    private final CarwashFlowService carwashFlowService;
    private final ExitGateService exitGateService;

    /**
     * [입차] 번호판 이미지 업로드 → 번호판 인식 → 기존고객 여부 → 차단봉 → ENTRY 로그
     */
    @PostMapping("/entry-image")
    public EntryResult handleEntryImage(
            @RequestParam("attach") MultipartFile attach
    ) throws Exception {
        String contentType = attach.getContentType();
        byte[] bytes = attach.getBytes();
        return carwashEntryService.handleEntryImage(contentType, bytes);
    }

    /**
     * [입차용 수동 차단봉 오픈]
     * - barrier만 올리고
     * - 게이트 OPEN 로그만 남기고
     * - EXIT 로그는 남기지 않는다.
     * 프런트에서 "차단봉 올리기" 누를 때 호출.
     */
    @PostMapping("/entry-gate-open")
    public ManualOpenResult entryGateOpen(
            @RequestParam("plate") String plate
    ) {
        return carwashEntryService.manualOpenForEntry(plate);
    }

    /**
     * [레시피 생성] 차량 전체 이미지 업로드 → 오염/색상 분석 → 세차 레시피 생성/실행
     */
    @PostMapping("/plan-image")
    public PlanAndExecuteResult planAndExecute(
            @RequestParam("plate") String plate,
            @RequestParam("attach") MultipartFile attach
    ) throws Exception {
        return carwashFlowService.planAndExecute(
                plate,
                attach.getContentType(),
                attach.getBytes()
        );
    }

    /**
     * [출차 게이트 제어] open/close
     * 이건 그대로 둔다.
     * - open일 때 barrier 올리고 GATE_OPEN + EXIT 로그까지 남김.
     * - close일 때 barrier 내리고 GATE_CLOSE 로그 남김.
     * -> 원래 쓰던 출차 처리 플로우에 영향 없음.
     */
    @PostMapping("/exit-gate")
    public GateActionResult exitGate(
            @RequestParam("plate") String plate,
            @RequestParam("action") String action
    ){
        return exitGateService.handleGateAction(plate, action);
    }
}
