package edu.sm.app.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Component
@Slf4j
public class Ai2IotTools {

    // 순환 참조 방지: 정적 변수로 디바이스 상태 관리
    private static final Map<String, Object> deviceStatus = new HashMap<>();

    @Tool(description = "현재 실내 온도를 섭씨(°C)로 반환합니다.")
    public int getCurrentTemperature() {
        Random random = new Random();
        int temp = 18 + random.nextInt(15);
        log.info("현재 온도: {}°C", temp);
        return temp;
    }

    @Tool(description = "난방 시스템을 제어합니다. targetTemperature가 0이면 중지, 양수면 해당 온도까지 가동")
    public String controlHeating(
            @ToolParam(description = "목표 온도 (0=중지)", required = true) int targetTemperature) {

        if (targetTemperature == 0) {
            log.info("난방 중지");
            updateDeviceStatus("heating", false);
            return "success";
        } else if (targetTemperature >= 18 && targetTemperature <= 30) {
            log.info("난방 {}°C 가동", targetTemperature);
            updateDeviceStatus("heating", true);
            return "success";
        }
        return "failure";
    }

    @Tool(description = "조명을 켜거나 끕니다.")
    public String controlLight(
            @ToolParam(description = "조명 켜기 여부", required = true) boolean turnOn) {

        log.info("조명 {}", turnOn ? "ON" : "OFF");
        updateDeviceStatus("light", turnOn);
        return "success";
    }

    @Tool(description = "환기 시스템을 제어합니다.")
    public String controlVentilation(
            @ToolParam(description = "환기 시작 여부", required = true) boolean start) {

        log.info("환기 {}", start ? "시작" : "중지");
        updateDeviceStatus("ventilation", start);
        return "success";
    }

    @Tool(description = "에어컨을 제어합니다. targetTemperature가 0이면 끄기, 양수면 냉방 시작")
    public String controlAirConditioner(
            @ToolParam(description = "목표 온도 (0=끄기)", required = true) int targetTemperature) {

        if (targetTemperature == 0) {
            log.info("에어컨 끄기");
            return "success";
        } else if (targetTemperature >= 18 && targetTemperature <= 28) {
            log.info("에어컨 {}°C 냉방", targetTemperature);
            return "success";
        }
        return "failure";
    }

    // 정적 메서드로 디바이스 상태 관리
    public static void updateDeviceStatus(String device, boolean status) {
        deviceStatus.put(device, status);
        log.info("디바이스 상태 업데이트: {} = {}", device, status);
    }

    public static Map<String, Object> getDeviceStatus() {
        if (deviceStatus.isEmpty()) {
            deviceStatus.put("heating", false);
            deviceStatus.put("light", false);
            deviceStatus.put("ventilation", false);
        }
        return new HashMap<>(deviceStatus);
    }
}