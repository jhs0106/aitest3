package edu.sm.controller;

import edu.sm.app.service.CarwashAdminService;
import edu.sm.app.dto.GateLogRow;
import edu.sm.app.dto.VehicleRow;
import edu.sm.app.dto.WashOrderRow;
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

    // 게이트 로그 최근 100건
    @GetMapping("/gate-logs")
    public List<GateLogRow> gateLogs() {
        return adminService.getGateLogs(100);
    }

    // 등록 차량 전체
    @GetMapping("/vehicles")
    public List<VehicleRow> vehicles() {
        return adminService.getVehicles();
    }

    // 최근 wash_order 20건 (각 단계 포함)
    @GetMapping("/orders")
    public List<WashOrderRow> orders() {
        return adminService.getWashOrdersFull(20);
    }
}

