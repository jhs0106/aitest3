package edu.sm.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarwashExitService {

    private final GateLogService gateLogService;
    private final BarrierControlService barrierControlService;

    /**
     * 출차 처리:
     *  - 차단봉 올려서 내보낸다
     *  - 출차 로그 남긴다
     */
    public ExitResult handleExit(String plate) {

        // 1. 출구 차단봉 개방
        barrierControlService.up();

        // 2. 출차 로그 기록
        gateLogService.logGateClose(plate);

        // 필요하면 여기서 wash_order 상태 DONE 업데이트 등 추가 가능

        ExitResult result = new ExitResult();
        result.setPlate(plate);
        result.setBarrier("UP");
        result.setMessage("출차 처리 완료");
        return result;
    }

    @lombok.Data
    public static class ExitResult {
        private String plate;
        private String barrier;
        private String message;
    }
}
