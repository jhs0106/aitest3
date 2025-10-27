package edu.sm.app.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailySummaryService {

    private final JdbcTemplate jdbc;

    /**
     * 오늘 기준 KPI를 한 번에 모아서 반환.
     * 이 값들은 /ai6/admin/summary/today 에서 그대로 내려가고
     * JSP(ownerreport.jsp 등)에서 fetch해서 KPI 카드에 뿌린다.
     */
    public SummaryDTO getTodaySummary() {

        // 1) 오늘 방문 차량 수
        // carwash_gate_log.event_type='ENTRY', logged_at = 오늘 날짜
        Integer visitCount = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM carwash_gate_log
            WHERE event_type = 'ENTRY'
              AND DATE(logged_at) = CURRENT_DATE
        """, Integer.class);

        if (visitCount == null) visitCount = 0;

        // 2) 오늘 매출 합계 (원)
        // wash_order.created_at 이 오늘인 건만 합산
        Integer totalRevenue = jdbc.queryForObject("""
            SELECT COALESCE(SUM(price), 0)
            FROM wash_order
            WHERE DATE(created_at) = CURRENT_DATE
        """, Integer.class);

        if (totalRevenue == null) totalRevenue = 0;

        // 3) 평균 단가 (원/건)
        Double avgTicket = jdbc.queryForObject("""
            SELECT COALESCE(AVG(price), 0)
            FROM wash_order
            WHERE DATE(created_at) = CURRENT_DATE
        """, Double.class);

        if (avgTicket == null) avgTicket = 0.0;

        // 4) 장비 이상 의심 카운트
        //
        // 아이디어:
        //   wash_log 에서 오늘 시작한 단계 중 pressure_bar 가
        //   너무 낮거나(NULL)인데 step_name 에 "세척", "헹굼", "rinse", "wash" 같은
        //   실제 분사/세정 단계로 보이는 것들을 count.
        //
        // 일단 heuristic:
        //   DATE(started_at)=오늘
        //   AND (pressure_bar IS NULL OR pressure_bar < 50)
        //   =>  '이상 의심'
        //
        // "bay" 단위 장비가 아직 DB에 없으니까 개수만 잡자.
        Integer suspiciousBayCount = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM wash_log
            WHERE DATE(started_at) = CURRENT_DATE
              AND (
                    pressure_bar IS NULL
                 OR pressure_bar < 50
              )
        """, Integer.class);

        if (suspiciousBayCount == null) suspiciousBayCount = 0;

        // 5) 휴면 단골 수
        //
        // 정의: vehicle 에 등록돼 있는 번호판 중
        //       - 최근에(오늘 포함) 최소 한 번이라도 'ENTRY' 찍힌 적 있음 = "단골 후보"
        //       - 그런데 최근 14일 안에 안 왔다 (즉 마지막 방문이 14일 전보다 더 오래됨)
        //
        // approach:
        //   1) plate별 마지막 방문 날짜(last_visit) = carwash_gate_log 중 ENTRY 최대 logged_at::date
        //   2) 그 last_visit 이 CURRENT_DATE - INTERVAL '14 days' 보다 오래되면 휴면
        //
        //   그리고 그 plate 가 vehicle 테이블에도 있어야 "단골로 관리중"이라고 판단.
        //
        Integer dormantCustomerCount = jdbc.queryForObject("""
            WITH last_visit AS (
                SELECT g.plate,
                       MAX(DATE(g.logged_at)) AS last_day
                FROM carwash_gate_log g
                WHERE g.event_type = 'ENTRY'
                GROUP BY g.plate
            )
            SELECT COUNT(*)
            FROM last_visit v
            JOIN vehicle veh ON veh.plate = v.plate
            WHERE v.last_day < (CURRENT_DATE - INTERVAL '14 days')
        """, Integer.class);

        if (dormantCustomerCount == null) dormantCustomerCount = 0;

        // DTO 채우기
        SummaryDTO dto = new SummaryDTO();
        dto.setVisitCount(visitCount);
        dto.setTotalRevenue(totalRevenue);
        dto.setAvgTicket(avgTicket.intValue()); // 정수로 반올림해서 주자. 카드에 그냥 "원" 붙여서 쓸 거니까
        dto.setSuspiciousBayCount(suspiciousBayCount);
        dto.setDormantCustomerCount(dormantCustomerCount);

        log.debug("[DailySummary] visit={}, revenue={}, avg={}, suspicious={}, dormant={}",
                visitCount, totalRevenue, avgTicket, suspiciousBayCount, dormantCustomerCount);

        return dto;
    }

    @Data
    public static class SummaryDTO {
        private int visitCount;            // 오늘 방문 차량 수
        private int totalRevenue;          // 오늘 매출 합계(원)
        private int avgTicket;             // 평균 단가(원/건, 정수화)
        private int suspiciousBayCount;    // 장비 이상 의심 건수
        private int dormantCustomerCount;  // 휴면 단골 수
    }
}
