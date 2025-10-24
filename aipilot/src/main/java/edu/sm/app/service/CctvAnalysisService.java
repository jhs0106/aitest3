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
          이미지를 분석하고 사용자 질문에 맞춰 한국어로 답변하세요.
          
          **가장 중요한 임무**: 만약 이미지에서 '화재'나 '심각한 사고'와 같이 인명 피해가 우려되는 '재난 상황'이 명확히 감지되면, 
          반드시 'EmergencyCallTool'의 'call112' 도구를 사용하여 신고 조치를 취하세요.
          도구 사용 후에는 분석 결과를 설명하는 내용을 포함하여 한국어로 답변을 마무리하세요.
          
          재난 상황이 아닐 경우(예: 일반적인 교통 상황, 일상적인 행위)는 도구를 사용하지 않고 질문에만 답변하세요.
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