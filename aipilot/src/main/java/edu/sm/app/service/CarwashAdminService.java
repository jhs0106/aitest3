package edu.sm.app.service;

import edu.sm.app.dto.GateLogRow;
import edu.sm.app.dto.VehicleRow;
import edu.sm.app.dto.WashOrderRow;
import edu.sm.app.dto.WashStepRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarwashAdminService {

    private final JdbcTemplate jdbc;

    /** 1) 게이트 로그 전체(또는 최근 N개) */
    public List<GateLogRow> getGateLogs(int limit) {
        return jdbc.query("""
            SELECT id, plate, event_type, logged_at
            FROM carwash_gate_log
            ORDER BY logged_at DESC
            LIMIT ?
        """, (rs, rowNum) -> {
            GateLogRow row = new GateLogRow();
            row.setId(rs.getLong("id"));
            row.setPlate(rs.getString("plate"));
            row.setEventType(rs.getString("event_type"));
            row.setLoggedAt(rs.getTimestamp("logged_at").toLocalDateTime());
            return row;
        }, limit);
    }

    /** 2) 등록된 차량 목록 */
    public List<VehicleRow> getVehicles() {
        return jdbc.query("""
            SELECT plate, size, color, last_wash_at
            FROM vehicle
            ORDER BY last_wash_at DESC NULLS LAST, plate ASC
        """, (rs, rowNum) -> {
            VehicleRow v = new VehicleRow();
            v.setPlate(rs.getString("plate"));
            v.setSize(rs.getString("size"));
            v.setColor(rs.getString("color"));
            if (rs.getTimestamp("last_wash_at") != null) {
                v.setLastWashAt(rs.getTimestamp("last_wash_at").toLocalDateTime());
            }
            return v;
        });
    }

    /** 3) wash_order + wash_log 묶어서 내려주기 */
    public List<WashOrderRow> getWashOrdersWithSteps(int limitOrders) {
        // 우선 주문들 가져옴 (최신순)
        List<WashOrderRow> orders = jdbc.query("""
            SELECT id, plate, status, price, eta_min, created_at
            FROM wash_order
            ORDER BY created_at DESC
            LIMIT ?
        """, (rs, rowNum) -> {
            WashOrderRow o = new WashOrderRow();
            o.setOrderId(rs.getString("id"));
            o.setPlate(rs.getString("plate"));
            o.setStatus(rs.getString("status"));
            o.setPrice((Integer) rs.getObject("price"));    // nullable
            o.setEtaMin((Integer) rs.getObject("eta_min")); // nullable
            o.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            return o;
        }, limitOrders);

        if (orders.isEmpty()) {
            return orders;
        }

        // order_id 목록 모아서 한 번에 wash_log 조회
        List<String> orderIds = orders.stream()
                .map(WashOrderRow::getOrderId)
                .toList();

        List<WashStepRow> allSteps = jdbc.query("""
            SELECT order_id,
                   step_idx,
                   step_name,
                   started_at,
                   ended_at,
                   pressure_bar,
                   chem_code,
                   result
            FROM wash_log
            WHERE order_id = ANY (?)
            ORDER BY order_id DESC, step_idx ASC
        """, (rs, rowNum) -> {
            WashStepRow s = new WashStepRow();
            s.setStepIdx(rs.getInt("step_idx"));
            s.setStepName(rs.getString("step_name"));
            if (rs.getTimestamp("started_at") != null) {
                s.setStartedAt(rs.getTimestamp("started_at").toLocalDateTime());
            }
            if (rs.getTimestamp("ended_at") != null) {
                s.setEndedAt(rs.getTimestamp("ended_at").toLocalDateTime());
            }
            s.setPressureBar((Integer) rs.getObject("pressure_bar"));
            s.setChemCode(rs.getString("chem_code"));
            s.setResult(rs.getString("result"));
            // order_id는 매핑에서 사용하니, 추후 groupBy로 붙임
            // 여기선 한 번에 못 넣었으므로 Map에서 처리
            return s;
        }, (Object) orderIds.toArray(new String[0]));

        // order_id별로 steps 묶기
        // 문제: 위 쿼리에서 order_id 안 넣어줬는데 필요하잖아.
        // 해결: 다시 수정 (포함해서 맵핑)
        // 위 쿼리 수정 ↓
        // (아래에서 다시 한 번 전체 메서드 수정할게)
        return attachStepsToOrders(orders);
    }

    private List<WashOrderRow> attachStepsToOrders(List<WashOrderRow> orders) {
        // 위에서 order/log join을 한 쿼리를 다시 작성해서 stepsByOrder 만들자.
        // (조금 다시 작성)

        List<String> orderIds = orders.stream()
                .map(WashOrderRow::getOrderId)
                .toList();

        // wash_log 다시 조회 (order_id 포함)
        List<Map<String, Object>> logs = jdbc.queryForList("""
            SELECT order_id,
                   step_idx,
                   step_name,
                   started_at,
                   ended_at,
                   pressure_bar,
                   chem_code,
                   result
            FROM wash_log
            WHERE order_id = ANY (?)
            ORDER BY order_id DESC, step_idx ASC
        """, (Object) orderIds.toArray(new String[0]));

        // order_id -> steps list
        Map<String, List<WashStepRow>> stepsByOrder = logs.stream()
                .map(row -> {
                    WashStepRow s = new WashStepRow();
                    s.setStepIdx((Integer) row.get("step_idx"));
                    s.setStepName((String) row.get("step_name"));

                    Object st = row.get("started_at");
                    if (st != null) {
                        s.setStartedAt(((java.sql.Timestamp)st).toLocalDateTime());
                    }
                    Object et = row.get("ended_at");
                    if (et != null) {
                        s.setEndedAt(((java.sql.Timestamp)et).toLocalDateTime());
                    }
                    s.setPressureBar((Integer) row.get("pressure_bar"));
                    s.setChemCode((String) row.get("chem_code"));
                    s.setResult((String) row.get("result"));

                    // order_id도 같이 담아주자
                    return Map.entry((String) row.get("order_id"), s);
                })
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));

        // orders에 steps 붙이기
        for (WashOrderRow o : orders) {
            o.setSteps( stepsByOrder.getOrDefault(o.getOrderId(), List.of()) );
        }

        return orders;
    }

    /**
     * 위의 getWashOrdersWithSteps()를 steps까지 완전히 붙인 버전으로 제공.
     */
    public List<WashOrderRow> getWashOrdersFull(int limitOrders) {
        // 1) 주문만 먼저
        List<WashOrderRow> orders = jdbc.query("""
            SELECT id, plate, status, price, eta_min, created_at
            FROM wash_order
            ORDER BY created_at DESC
            LIMIT ?
        """, (rs, rowNum) -> {
            WashOrderRow o = new WashOrderRow();
            o.setOrderId(rs.getString("id"));
            o.setPlate(rs.getString("plate"));
            o.setStatus(rs.getString("status"));
            o.setPrice((Integer) rs.getObject("price"));
            o.setEtaMin((Integer) rs.getObject("eta_min"));
            o.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            return o;
        }, limitOrders);

        // 2) wash_log 붙이기
        return attachStepsToOrders(orders);
    }
}

