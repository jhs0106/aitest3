package edu.sm.controller;

import edu.sm.app.service.CarwashEntryService;
import edu.sm.app.service.CarwashEntryService.EntryResult;
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
     */
    @PostMapping("/exit-gate")
    public GateActionResult exitGate(
            @RequestParam("plate") String plate,
            @RequestParam("action") String action
    ){
        return exitGateService.handleGateAction(plate, action);
    }
}
