package edu.sm.app.service;

import edu.sm.app.tool.DoorControlTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;

// 얼굴 인식 및 도어 제어를 위한 AI 서비스
@Service
@Slf4j
public class FaceRecognitionService {
    private final ChatClient chatClient;
    private final DoorControlTool doorControlTool;
    private final DoorUserService doorUserService; // 변경: MockImpl -> DoorUserService

    public FaceRecognitionService(ChatModel chatModel, DoorControlTool doorControlTool, DoorUserService doorUserService) { // 생성자 매개변수 변경
        this.chatClient = ChatClient.builder(chatModel).build();
        this.doorControlTool = doorControlTool;
        this.doorUserService = doorUserService;
    }

    // 이미지로부터 얼굴 특징을 텍스트로 추출하는 AI 서비스 (생략된 부분 동일)
    public String extractFaceSignature(String name, String contentType, byte[] bytes) {
        log.info("AI 얼굴 특징 추출 시작: {}", name);

        Media media = Media.builder()
                .mimeType(MimeType.valueOf(contentType))
                .data(new ByteArrayResource(bytes))
                .build();

        // AI에게 이미지에서 사람의 특징을 추출하도록 요청
        String signature = chatClient.prompt()
                .system("""
                    당신은 이미지 분석 전문가입니다. 주어진 이미지를 분석하고,
                    인물(얼굴, 머리 모양, 옷 색상 등)을 상세히 설명하는 텍스트 50자 이내를 생성하세요.
                    다른 설명 없이 오직 텍스트 특징(예: '흰 티셔츠를 입은 긴 머리의 여성')만 응답해야 합니다.
                """)
                .user(userSpec -> userSpec.text("이 이미지를 분석하고 인물의 특징을 설명하세요.").media(media))
                .call()
                .content();

        log.info("AI 특징 추출 결과: {}", signature);
        return signature;
    }


    // 이미지로부터 사용자를 인식하고 문을 제어하는 AI 서비스 (생략된 부분 동일)
    public Flux<String> recognizeAndControl(MultipartFile attach) throws IOException {

        // 1. 등록된 사용자 목록 가져오기 (AI에게 Context로 제공)
        String registeredUsers = doorUserService.getAllFaceSignatures();

        // 2. 미디어 및 사용자 메시지 생성
        Media media = Media.builder()
                .mimeType(MimeType.valueOf(attach.getContentType()))
                .data(new ByteArrayResource(attach.getBytes()))
                .build();

        String recognitionPrompt = String.format("""
            제공된 이미지에 보이는 인물의 얼굴 특징을 분석하세요.
            이 인물의 특징이 다음 등록된 사용자 목록의 'Signature'와 가장 유사한지 비교하고,
            유사도가 0.7 이상이라고 판단되면 해당 사용자의 'Name'과 'Confidence' 1.0을 사용하여 'openDoorAndLog' 도구를 호출하세요.
            유사한 인물이 없거나 유사도 0.7 미만이라고 판단되면, 'closeDoorAndLogFailure' 도구를 호출하세요.
            
            --- 등록된 사용자 목록 ---
            %s
            ---
            
            출입문 제어 도구를 호출한 후, 사용자에게 결과를 한국어로 친절하게 설명하세요.
            """, registeredUsers);

        UserMessage userMessage = UserMessage.builder()
                .text(recognitionPrompt)
                .media(media)
                .build();

        // 3. LLM에 요청하고, 응답받기 (도구 사용 포함)
        Flux<String> flux = chatClient.prompt()
                .messages(new SystemMessage("당신은 AI 얼굴 인식 기반 출입문 보안 시스템입니다. 상황에 맞게 도구를 호출하고 결과를 응답하세요."), userMessage)
                .tools(doorControlTool) // DoorControlTool 연결
                .stream()
                .content();
        return flux;
    }
}