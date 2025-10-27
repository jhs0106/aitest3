package edu.sm.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarwashEntryService {

    private final ChatModel chatModel;
    private final JdbcCarRegistry jdbcCarRegistry;
    private final BarrierControlService barrierControlService;
    private final GateLogService gateLogService;

    /** 프런트: /ai6/entry-image 에서 호출 */
    public EntryResult handleEntryImage(String contentType, byte[] imageBytes) {

        // 1. 번호판 인식
        String plate = recognizePlate(contentType, imageBytes);
        plate = (plate != null) ? plate.replaceAll("\\s+","") : null;

        if (plate == null || plate.isBlank()) {
            // 인식 실패 -> 차단봉 닫음 유지
            log.warn("[ENTRY] plate 인식 실패. barrier CLOSE 유지.");
            barrierControlService.down();

            EntryResult failRes = new EntryResult();
            failRes.setPlate("(UNKNOWN)");
            failRes.setKnown(false);
            failRes.setBarrier("DOWN");
            return failRes;
        }

        // 2. 기존 고객 여부
        boolean alreadyKnown = jdbcCarRegistry.isKnownCar(plate);

        // 3. 신규면 INSERT / 기존이면 무시
        jdbcCarRegistry.upsertVehicleOnEntry(plate);

        // 4. 정책: 기존 고객이면 자동 오픈, 신규면 닫아둠
        if (alreadyKnown) {
            barrierControlService.up();
        } else {
            barrierControlService.down();
        }

        // 5. 로그
        gateLogService.logEntry(plate);
        if (alreadyKnown) {
            gateLogService.logGateOpen(plate);
        } else {
            gateLogService.logGateClose(plate);
        }

        // 6. 결과 리턴
        EntryResult result = new EntryResult();
        result.setPlate(plate);
        result.setKnown(alreadyKnown);
        result.setBarrier(alreadyKnown ? "UP" : "DOWN");
        return result;
    }

    /**
     * 입차 상황에서 사람이 "차단봉 올리기"를 눌렀을 때 호출.
     * - barrier만 올린다.
     * - GATE_OPEN 로그만 남긴다.
     * - 절대 EXIT 로그는 찍지 않는다. (차량은 아직 안 나갔으니까)
     */
    public ManualOpenResult manualOpenForEntry(String plate) {
        // 실제 차단봉 제어
        barrierControlService.up();

        // 게이트 오픈 기록만 남김
        gateLogService.logGateOpen(plate);

        ManualOpenResult res = new ManualOpenResult();
        res.setPlate(plate);
        res.setBarrier("UP");
        res.setMessage("입차 차단봉 수동 개방 완료");
        return res;
    }

    /** LLM 비전으로 번호판 문자열만 추출 */
    private String recognizePlate(String contentType, byte[] bytes) {
        Media media = Media.builder()
                .mimeType(MimeType.valueOf(contentType))
                .data(new ByteArrayResource(bytes))
                .build();

        UserMessage userMessage = UserMessage.builder()
                .text("""
                    이미지에서 한국 자동차 번호판을 인식하세요.
                    예: "12가3456", "157고4895"
                    다른 단어나 설명 없이, 번호판만 한 줄로 출력하세요.
                """)
                .media(media)
                .build();

        ChatClient chatClient = ChatClient.builder(chatModel).build();

        String raw = chatClient
                .prompt()
                .messages(userMessage)
                .call()
                .content();

        if (raw == null) return null;
        String plate = raw.trim().replaceAll("\\s+","");

        log.info("[ENTRY] 인식된 번호판 plate={}", plate);
        return plate;
    }

    @lombok.Data
    public static class EntryResult {
        private String plate;    // "157고4895"
        private boolean known;   // 이미 등록된 차량인지
        private String barrier;  // "UP" / "DOWN"
    }

    @lombok.Data
    public static class ManualOpenResult {
        private String plate;    // "157고4895"
        private String barrier;  // "UP" / "DOWN"
        private String message;  // "입차 차단봉 수동 개방 완료"
    }
}
