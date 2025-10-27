package edu.sm.app.service;

import edu.sm.app.tool.EmergencyCallTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class CctvAnalysisService {
    private final ChatClient chatClient;

    @Autowired
    private EmergencyCallTool emergencyCallTool;

    public CctvAnalysisService(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    // ##### CCTV 영상 분석 메소드 #####
    public Flux<String> analyzeAndRespond(String question, String contentType, byte[] bytes) {
        // 시스템 메시지 생성: 재난 감지 및 도구 사용 지시
        SystemMessage systemMessage = SystemMessage.builder()
                .text("""
          당신은 CCTV 영상 분석 전문가이며 재난 상황 감시 요원입니다.
          
          **가장 중요한 임무**: 이미지에서 '화재', '심각한 사고', '응급상황'과 같이 인명 피해가 우려되는 '재난 상황'이 명확히 감지되면, 
          반드시 'EmergencyCallTool'의 'call119' 도구를 호출하여 신고 조치를 취하세요.
          
          **출력 규칙**:
          1. 재난 상황이 감지되면: 도구의 응답(119 신고 시뮬레이션 결과)과 감지한 재난 유형만 포함하여 한국어로 간결하게 응답하세요. (예: "🚨 119에 신고하는 중... [시뮬레이션] 재난 유형: 화재, 위치/상황: ...")
          2. 재난 상황이 감지되지 않으면: 다른 어떤 텍스트나 설명도 없이, 오직 'NO_DISASTER_DETECTED'라는 텍스트만 출력하세요.
        """)
                .build();

        // 미디어 및 사용자 메시지 생성
        Media media = Media.builder()
                .mimeType(MimeType.valueOf(contentType))
                .data(new ByteArrayResource(bytes))
                .build();
        UserMessage userMessage = UserMessage.builder()
                .text(question)
                .media(media)
                .build();

        // LLM에 요청하고, 응답받기
        Flux<String> flux = chatClient.prompt()
                .messages(systemMessage, userMessage)
                .tools(emergencyCallTool) // 새로 만든 도구 연결
                .stream()
                .content();
        return flux;
    }
}