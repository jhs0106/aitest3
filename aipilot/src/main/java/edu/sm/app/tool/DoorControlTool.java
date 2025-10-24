package edu.sm.app.tool;

import edu.sm.app.service.DoorUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// AI가 호출하여 문을 제어하고 기록을 남기는 Tool
@Component
@Slf4j
public class DoorControlTool {

    // DoorUserService로 변경
    @Autowired
    private DoorUserService doorUserService;

    @Tool(description = "등록된 사용자의 이름을 받아 출입문을 열고 성공적으로 출입 기록을 남깁니다.")
    public String openDoorAndLog(
            @ToolParam(description = "인식된 사용자의 이름", required = true) String userName,
            @ToolParam(description = "AI가 판단한 사용자 얼굴 특징의 정확도 (1.0에 가까울수록 정확)", required = true) double confidence) {

        if (confidence < 0.7) {
            log.warn("🚨 인식 정확도 낮음: {} ({})", userName, confidence);
            doorUserService.logAccess(userName, "FAILED");
            return String.format("인식 정확도(%.2f)가 낮아 문을 열 수 없습니다. 출입 실패 기록을 남겼습니다.", confidence);
        }

        log.info("🔓 출입문 개방 요청: 사용자 - {}", userName);
        doorUserService.logAccess(userName, "SUCCESS");
        return String.format("✅ %s님의 얼굴이 확인되어 출입문을 개방합니다. 출입 기록이 성공적으로 저장되었습니다.", userName);
    }

    @Tool(description = "등록되지 않은 사용자이거나 인식에 실패했을 때 문을 닫고 실패 기록을 남깁니다.")
    public String closeDoorAndLogFailure(
            @ToolParam(description = "미인식된 경우 'Unknown' 또는 인식하려 했던 이름", required = true) String attemptedName) {

        log.info("🔒 출입문 폐쇄 및 실패 기록: 사용자 - {}", attemptedName);
        doorUserService.logAccess(attemptedName, "FAILED");
        return String.format("❌ %s님의 얼굴을 데이터베이스에서 찾을 수 없습니다. 출입문은 열리지 않습니다. 출입 실패 기록을 남겼습니다.", attemptedName);
    }
}