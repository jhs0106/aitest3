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

    /**
     * 입차 처리:
     *  1) 번호판 인식
     *  2) vehicle 테이블에서 기존고객 여부 확인
     *  3) 차단봉 올릴지 / 내릴지 결정
     *  4) ENTRY 로그 기록
     *  5) plate, known, barrier 상태를 반환
     */
    public EntryResult handleEntryImage(String contentType, byte[] imageBytes) {

        // 1. 번호판 인식 (LLM 비전)
        String plate = recognizePlate(contentType, imageBytes);

        // 2. 기존고객 여부: vehicle 테이블 확인
        boolean known = jdbcCarRegistry.isKnownCar(plate);

        // (선택) 만약 신규 고객도 vehicle에 저장해 두고 싶으면:
        // jdbcCarRegistry.upsertVehicleOnEntry(plate);

        // 3. 차단봉 제어
        if (known) {
            barrierControlService.up();      // 기존 고객이면 바로 열어준다
        } else {
            barrierControlService.down();    // 신규면 일단 닫아둔다(혹은 열어도 되고 정책에 따라)
        }

        // 4. 입차 로그 기록 (ENTRY)
        gateLogService.logEntry(plate);

        // 5. 결과 DTO 구성
        EntryResult result = new EntryResult();
        result.setPlate(plate);
        result.setKnown(known);
        result.setBarrier(known ? "UP" : "DOWN"); // 프론트에서 "차단봉:" 옆에 뿌릴 용도
        return result;
    }

    /**
     * LLM으로 이미지에서 번호판만 추출
     */
    private String recognizePlate(String contentType, byte[] bytes) {
        Media media = Media.builder()
                .mimeType(MimeType.valueOf(contentType))
                .data(new ByteArrayResource(bytes))
                .build();

        UserMessage userMessage = UserMessage.builder()
                .text("""
                    이미지에서 자동차 번호판을 인식하세요.
                    한국 번호판 형식(예: '12가3456', '157고4895')만 추출하고,
                    다른 설명 없이 그 번호판만 텍스트로 반환하세요.
                """)
                .media(media)
                .build();

        ChatClient chatClient = ChatClient.builder(chatModel).build();

        String raw = chatClient
                .prompt()
                .messages(userMessage)
                .call()
                .content()
                .trim();

        // 혹시 공백 섞여있으면 제거
        String plate = raw.replaceAll("\\s+", "");
        log.info("[ENTRY] 인식된 번호판 plate={}", plate);
        return plate;
    }

    @lombok.Data
    public static class EntryResult {
        private String plate;    // 인식된 번호판
        private boolean known;   // 기존고객 여부 (vehicle에 있으면 true)
        private String barrier;  // 'UP' or 'DOWN'
    }
}
