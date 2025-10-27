package edu.sm.app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JdbcCarRegistry {

    private final JdbcTemplate jdbc;

    // 1) 지금 이 차가 DB에 이미 있는지?
    public boolean isKnownCar(String plate) {
        Integer cnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM vehicle WHERE plate = ?",
                Integer.class,
                plate
        );
        return (cnt != null && cnt > 0);
    }

    // 2) 차가 없으면 INSERT 해서 vehicle에 등록
    public void upsertVehicleOnEntry(String plate) {
        jdbc.update("""
            INSERT INTO vehicle (plate, last_wash_at)
            VALUES (?, NOW())
            ON CONFLICT (plate) DO NOTHING
        """, plate);
    }

    // (참고) 세차 끝났을 때 색상/사이즈 업데이트하는 용도로도 이런 메서드가 있었지?
    // public void updateVehicleAfterWash(... ) { ... }
}
