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
            // 인식 실패 시: 차단봉은 닫고, known=false 로 응답
            log.warn("[ENTRY] plate 인식 실패. barrier CLOSE 유지.");
            barrierControlService.down();

            // 로그: 최소한 시도한건 ENTRY로 남겨줄 수도 있고, 애매하면 안 남겨도 된다.
            // 여기선 아예 plate 없이 남기는 건 의미 없으니 스킵

            EntryResult failRes = new EntryResult();
            failRes.setPlate("(UNKNOWN)");
            failRes.setKnown(false);
            failRes.setBarrier("DOWN");
            return failRes;
        }

        // 2. 기존 고객 여부 확인
        boolean alreadyKnown = jdbcCarRegistry.isKnownCar(plate);

        // 3. 신규라면 vehicle에 INSERT (ON CONFLICT DO NOTHING -> 기존이면 무시)
        jdbcCarRegistry.upsertVehicleOnEntry(plate);

        // 4. 차단봉 제어 정책
        if (alreadyKnown) {
            barrierControlService.up();     // 기존 고객이면 자동 오픈
        } else {
            barrierControlService.down();   // 신규면 우선 닫아둔다 (정책에 따라 바꿀 수 있음)
        }

        // 5. 게이트 로그 기록
        gateLogService.logEntry(plate);
        if (alreadyKnown) {
            gateLogService.logGateOpen(plate);   // known 고객: 열린 상태
        } else {
            gateLogService.logGateClose(plate);  // unknown 고객: 닫힌 상태
        }

        // 6. 결과 DTO 만들어서 반환
        EntryResult result = new EntryResult();
        result.setPlate(plate);
        result.setKnown(alreadyKnown);
        result.setBarrier(alreadyKnown ? "UP" : "DOWN");
        return result;
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
        private boolean known;   // DB에 이미 등록돼 있던 차인지 여부
        private String barrier;  // "UP" 또는 "DOWN"
    }
}
