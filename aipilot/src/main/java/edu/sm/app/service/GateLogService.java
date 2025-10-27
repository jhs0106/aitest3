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
        // plate 정리 (공백 제거)
        String norm = (plate != null) ? plate.replaceAll("\\s+", "") : null;

        jdbc.update("""
            INSERT INTO carwash_gate_log (plate, event_type)
            VALUES (?, ?)
        """, norm, eventType);

        log.info("[GATELOG] {} - {}", norm, eventType);
    }

    /** 입차 시점 기록 */
    public void logEntry(String plate) {
        insertLog(plate, "ENTRY");
    }

    /** 출차 시점 기록 */
    public void logExit(String plate) {
        insertLog(plate, "EXIT");
    }

    /** 차단봉/게이트 열림 기록 */
    public void logGateOpen(String plate) {
        insertLog(plate, "GATE_OPEN");
    }

    /** 차단봉/게이트 닫힘 기록 */
    public void logGateClose(String plate) {
        insertLog(plate, "GATE_CLOSE");
    }
}
