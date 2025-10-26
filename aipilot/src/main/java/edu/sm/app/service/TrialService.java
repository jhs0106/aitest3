package edu.sm.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Step 1: 기본 법정 채팅 서비스
 *
 * 기능:
 * - AI 판사와 대화
 * - 스트리밍 응답
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TrialService {

    private final ChatClient.Builder chatClientBuilder;

    /**
     * AI 판사와 채팅
     *
     * @param message 사용자 메시지
     * @return AI 응답 (스트리밍)
     */
    public Flux<String> chat(String message) {
        log.info("사용자 메시지: {}", message);

        ChatClient chatClient = chatClientBuilder.build();

        return chatClient.prompt()
                .system("""
                    당신은 대한민국 법정의 판사입니다.
                    
                    역할:
                    - 정중하고 공정하게 대화합니다
                    - 법률 용어를 쉽게 설명합니다
                    - 재판 절차를 안내합니다
                    
                    말투:
                    - "~합니다" 형식 사용
                    - 존댓말 사용
                    """)
                .user(message)
                .stream()
                .content();
    }
}