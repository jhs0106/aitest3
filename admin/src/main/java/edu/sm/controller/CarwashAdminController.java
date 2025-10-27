package edu.sm.controller;

import edu.sm.app.dto.GateLogRow;
import edu.sm.app.dto.VehicleRow;
import edu.sm.app.dto.WashOrderRow;
import edu.sm.app.service.CarwashAdminService;
import edu.sm.app.service.DailySummaryService;
import edu.sm.app.service.OwnerReportService;
import edu.sm.app.service.OwnerReportService.OwnerAnswer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai6/admin")
@RequiredArgsConstructor
@Slf4j
public class CarwashAdminController {

    private final CarwashAdminService adminService;
    private final DailySummaryService dailySummaryService;
    private final OwnerReportService ownerReportService;

    /**
     * KPI 카드용: 오늘 방문/매출/평균단가/이상부스/휴면단골 등
     * 프론트에서 GET /ai6/admin/summary/today 로 호출해야 함
     */
    @GetMapping("/summary/today")
    public DailySummaryService.SummaryDTO getTodaySummary() {
        return dailySummaryService.getTodaySummary();
    }

    /**
     * 사장 질문 -> AI 분석 보고
     * 프론트에서 POST /ai6/admin/owner-ask?question=... 로 호출해야 함
     */
    @PostMapping("/owner-ask")
    public OwnerAnswer askOwner(@RequestParam("question") String question) {
        log.info("[OWNER-ASK] question={}", question);
        // 서비스 메서드 이름 실제 프로젝트에 맞추기
        return ownerReportService.askOwnerAI(question);
        // 만약 서비스가 askOwner(...) 라면 위 줄 대신:
        // return ownerReportService.askOwner(question);
    }

    /**
     * 게이트 로그 최근 100건
     * (관리자용 테이블 뿌리는 데서 쓰는 거)
     */
    @GetMapping("/gate-logs")
    public List<GateLogRow> gateLogs() {
        return adminService.getGateLogs(100);
    }

    /**
     * 등록 차량 전체
     */
    @GetMapping("/vehicles")
    public List<VehicleRow> vehicles() {
        return adminService.getVehicles();
    }

    /**
     * 최근 wash_order 20건 (각 단계 포함)
     */
    @GetMapping("/orders")
    public List<WashOrderRow> orders() {
        return adminService.getWashOrdersFull(20);
    }
}
