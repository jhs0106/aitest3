package edu.sm.app.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BarrierControlService {

    public void up() {
        // 실제 하드웨어 제어 신호 나가는 자리
        log.info("[BARRIER] 차단봉 올림 (게이트 OPEN)");
    }

    public void down() {
        log.info("[BARRIER] 차단봉 내림 (게이트 CLOSE)");
    }
}
