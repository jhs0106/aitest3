package edu.sm.controller;

import edu.sm.app.service.TrialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 모의 법정 API 컨트롤러
 *
 * 엔드포인트:
 * - GET /trial/chat?message=안녕하세요
 */
@RestController
@RequestMapping("/trial")
@Slf4j
@RequiredArgsConstructor
public class TrialController {

    private final TrialService trialService;

    /**
     * AI 판사와 채팅
     *
     * 사용 예:
     * http://localhost:8445/trial/chat?message=안녕하세요
     *
     * @param message 사용자 메시지
     * @return AI 응답 (스트리밍)
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam("message") String message) {
        log.info("채팅 요청 - 메시지: {}", message);
        return trialService.chat(message);
    }
}