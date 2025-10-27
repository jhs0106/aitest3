package edu.sm.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExitGateService {

    private final BarrierControlService barrierControlService;
    private final GateLogService gateLogService;

    public GateActionResult handleGateAction(String plate, String action) {
        // action: "open" or "close"
        GateActionResult result = new GateActionResult();
        result.setPlate(plate);

        if ("open".equalsIgnoreCase(action)) {
            // 차단봉 올려서 차를 내보낼 준비
            barrierControlService.up();

            // 로그: 게이트 열림
            gateLogService.logGateOpen(plate);

            // 차가 실제로 나갔다고 간주하면 EXIT도 같이 남겨주자
            gateLogService.logExit(plate);

            result.setGate("OPEN");
            result.setMessage("출차 게이트 개방 및 출차 처리 완료");

        } else if ("close".equalsIgnoreCase(action)) {
            // 차단봉 닫음
            barrierControlService.down();

            // 로그: 게이트 닫힘
            gateLogService.logGateClose(plate);

            result.setGate("CLOSE");
            result.setMessage("출차 게이트 닫힘 완료");

        } else {
            result.setGate("UNKNOWN");
            result.setMessage("지원하지 않는 action: " + action);
        }

        return result;
    }

    @lombok.Data
    public static class GateActionResult {
        private String plate;
        private String gate;     // "OPEN", "CLOSE", etc.
        private String message;  // 간단한 설명
    }
}
