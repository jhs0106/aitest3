package edu.sm.app.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 차량 등록 여부 확인용 도구
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VehicleCheckTools {

    private final JdbcTemplate jdbcTemplate;

    @Tool(description = "차량 번호가 등록된 고객 차량인지 확인합니다. 등록이면 true, 아니면 false.")
    public boolean checkPlateRegistration(
            @ToolParam(description = "차량 번호판, 예: '12가3456'") String plate
    ) {
        String normalized = plate.replaceAll("\\s+", "");
        log.info("[TOOL] checkPlateRegistration called with {}", normalized);

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from vehicle where plate = ?",
                Integer.class,
                normalized
        );
        boolean registered = (count != null && count > 0);
        log.info("[TOOL] checkPlateRegistration result={} for {}", registered, normalized);
        return registered;
    }
}
