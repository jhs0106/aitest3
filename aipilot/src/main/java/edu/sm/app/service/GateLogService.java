package edu.sm.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GateLogService {

    private final JdbcTemplate jdbc;

    private void insertLog(String plate, String eventType) {
        log.info("[GATE-LOG] {} plate={}", eventType, plate);
        jdbc.update("""
            INSERT INTO carwash_gate_log (plate, event_type)
            VALUES (?, ?)
        """, plate, eventType);
    }

    /** 입차 기록 */
    public void logEntry(String plate) {
        insertLog(plate, "ENTRY");
    }

    /** 출차 기록 (차가 나간 시점) */
    public void logExit(String plate) {
        insertLog(plate, "EXIT");
    }

    /** 게이트 열림 기록 */
    public void logGateOpen(String plate) {
        insertLog(plate, "GATE_OPEN");
    }

    /** 게이트 닫힘 기록 */
    public void logGateClose(String plate) {
        insertLog(plate, "GATE_CLOSE");
    }
}
