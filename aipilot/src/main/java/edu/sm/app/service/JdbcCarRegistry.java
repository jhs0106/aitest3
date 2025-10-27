package edu.sm.app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JdbcCarRegistry {

    private final JdbcTemplate jdbc;

    /**
     * vehicle 테이블에 해당 번호판이 있으면 기존 고객으로 본다.
     */
    public boolean isKnownCar(String plate) {
        Integer cnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM vehicle WHERE plate = ?",
                Integer.class,
                plate
        );
        return (cnt != null && cnt > 0);
    }

    /**
     * 만약 plate가 아직 없다면 신규고객이니까 vehicle에 최소 정보라도 넣어준다.
     * 이건 선택적(아래에서 어디서 호출할지 결정)
     */
    public void upsertVehicleOnEntry(String plate) {
        // 이미 있으면 아무것도 안 하고,
        // 없으면 최소 row를 만든다 (처음 본 차라도 이후 단계에서 쓸 수 있게)
        jdbc.update("""
            INSERT INTO vehicle (plate, last_wash_at)
            VALUES (?, NOW())
            ON CONFLICT (plate) DO NOTHING
        """, plate);
    }
}
