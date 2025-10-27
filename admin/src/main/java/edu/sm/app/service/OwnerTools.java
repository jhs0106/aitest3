package edu.sm.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 사장 전용 액션들.
 * 나중에 OwnerReportService에서 ChatClient.prompt().tools(...)로 끼워넣으면
 * "점검 요청할까요?" → 실제 insert까지 자동으로 가능.
 *
 * 지금 단계에서는 아직 OwnerReportService에 연결 안 하고,
 * 나중 단계에서 붙일 수 있게만 준비해둠.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OwnerTools {

    private final JdbcTemplate jdbcTemplate;

    // 1) 장비 점검 예약
    @Tool(description = "특정 세차 부스/라인의 점검 요청을 등록합니다.")
    public String scheduleBayCheck(
            @ToolParam(description = "부스 이름 또는 ID", required = true) String bay
    ) {
        jdbcTemplate.update("""
            INSERT INTO maintenance_ticket (target, issue_desc)
            VALUES (?, '자동 점검 요청')
        """, bay);

        log.info("[OWNER TOOL] scheduleBayCheck -> {}", bay);
        return "점검 요청 등록 완료 ("+bay+")";
    }

    // 2) 휴면 고객 쿠폰 발송(더미)
    @Tool(description = "2주 이상 방문 없는 고객에게 할인 쿠폰 발송 요청을 등록합니다.")
    public String sendCouponToDormantCustomers(
            @ToolParam(description = "할인율(%)", required = true) int discountPercent
    ) {
        jdbcTemplate.update("""
            INSERT INTO marketing_task (task_type, payload)
            VALUES ('DORMANT_COUPON', ?)
        """, "할인율="+discountPercent+"%");
        log.info("[OWNER TOOL] sendCouponToDormantCustomers -> {}%", discountPercent);
        return "휴면 고객 대상 " + discountPercent + "% 쿠폰 발송 캠페인 요청 완료";
    }
}
