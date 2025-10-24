package edu.sm.app.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmergencyCallTool {

    @Tool(description = "화재, 사고와 같은 재난 상황이 발생했을 때 112에 신고하는 기능을 시뮬레이션 합니다.")
    public String call112(
            @ToolParam(description = "재난 유형 (예: 화재, 사고, 응급상황)", required = true) String disasterType,
            @ToolParam(description = "CCTV 위치 또는 상황 설명", required = true) String location) {

        String confirmation = String.format(
                "112에 신고하는 중... [시뮬레이션] 재난 유형: %s, 위치/상황: %s",
                disasterType, location);
        log.warn(confirmation); // 실제 신고 대신 로그 기록
        return "🚨 " + confirmation; // AI 응답에 포함될 문자열
    }
}