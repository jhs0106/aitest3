package edu.sm.app.service;

import edu.sm.app.tool.Ai2IotTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.Ordered;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class Ai2IntegratedService {

    private final ChatClient.Builder chatClientBuilder;
    private final ChatMemory chatMemory;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final OpenAiAudioTranscriptionModel transcriptionModel;
    private final OpenAiAudioSpeechModel speechModel;
    private final Ai2IotTools ai2IotTools;

    // ===== 센서 데이터 =====
    public Map<String, Object> getSensorData() {
        Random random = new Random();
        Map<String, Object> data = new HashMap<>();
        data.put("temperature", 18 + random.nextInt(15));
        data.put("humidity", 40 + random.nextInt(40));
        data.put("light", 100 + random.nextInt(900));
        return data;
    }

    // ===== 음성 명령 처리 =====
    public Map<String, String> processVoiceCommand(
            MultipartFile speech, String conversationId) throws IOException {

        // STT
        String command = stt(speech);
        log.info("STT: {}", command);

        // IoT 제어
        String result = executeIotCommand(command, conversationId);

        // TTS
        byte[] audio = tts(result);
        String base64Audio = Base64.getEncoder().encodeToString(audio);

        Map<String, String> response = new HashMap<>();
        response.put("command", command);
        response.put("result", result);
        response.put("audio", base64Audio);
        return response;
    }

    // ===== 텍스트 명령 처리 =====
    public Map<String, String> processTextCommand(
            String command, String conversationId) {
        try {
            log.info("텍스트 명령 수신: {}", command);
            String result = executeIotCommand(command, conversationId);
            log.info("텍스트 명령 처리 완료 - command: {}, result: {}", command, result);

            Map<String, String> response = new HashMap<>();
            response.put("command", command);  // ← 이 줄이 핵심!
            response.put("result", result != null ? result : "응답을 생성하지 못했습니다.");
            return response;
        } catch (Exception e) {
            log.error("텍스트 명령 처리 중 오류 발생: command={}", command, e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("command", command);
            errorResponse.put("result", "명령 처리 중 오류가 발생했습니다.");
            return errorResponse;
        }
    }

    // ===== IoT 명령 실행 =====
    private String executeIotCommand(String command, String conversationId) {
        try {
            log.info("IoT 명령 실행 시작 - command: {}, conversationId: {}", command, conversationId);

            ChatClient chatClient = chatClientBuilder.build();

            String answer = chatClient.prompt()
                    .system("""
                        당신은 스마트홈 AI 어시스턴트입니다.
                        사용자의 명령을 분석하여 적절한 IoT 기기를 제어하세요.
                        
                        예시:
                        - "춥다", "추워" → 난방 가동
                        - "덥다", "더워" → 에어컨 가동 또는 난방 중지
                        - "불 켜줘" → 조명 ON
                        - "환기 시작" → 환기 시스템 가동
                        
                        이전 대화를 기억하며 사용자 선호도를 학습하세요.
                        응답은 간결하고 친절하게 한국어로 작성하세요.
                        """)
                    .user(command)
                    .tools(ai2IotTools)
                    .advisors(
                            PromptChatMemoryAdvisor.builder(chatMemory).build(),
                            new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1)
                    )
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .content();

            // ✅ 수정: null 체크 및 기본값 제공
            if (answer == null || answer.trim().isEmpty()) {
                log.warn("AI 응답이 비어있음 - command: {}", command);
                return "명령을 처리했지만 응답을 생성하지 못했습니다.";
            }

            log.info("IoT 명령 실행 완료 - answer 길이: {}", answer.length());
            return answer;

        } catch (Exception e) {
            log.error("IoT 명령 실행 중 오류 발생 - command: {}", command, e);
            return "명령 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
        }
    }


    // ===== RAG 검색 =====
    public String searchManual(String question) {
        SearchRequest searchRequest = SearchRequest.builder()
                .similarityThreshold(0.5)
                .topK(3)
                .build();

        QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .build();

        ChatClient chatClient = chatClientBuilder.build();

        return chatClient.prompt()
                .system("""
                        당신은 IoT 기기 매뉴얼 전문가입니다.
                        제공된 문서를 바탕으로 정확하게 답변하세요.
                        문서에 정보가 없으면 "매뉴얼에 해당 정보가 없습니다"라고 답하세요.
                        """)
                .user(question)
                .advisors(advisor)
                .call()
                .content();
    }

    // ===== Memory 채팅 =====
    public Flux<String> chatWithMemory(String message, String conversationId) {
        ChatClient chatClient = chatClientBuilder.build();

        return chatClient.prompt()
                .system("""
                        당신은 친절한 스마트홈 AI 어시스턴트입니다.
                        이전 대화를 기억하며 자연스럽게 대화하세요.
                        """)
                .user(message)
                .advisors(PromptChatMemoryAdvisor.builder(chatMemory).build())
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

    // ===== 문서 업로드 =====
    public String uploadDocument(MultipartFile attach, String type) throws IOException {
        Resource resource = new ByteArrayResource(attach.getBytes());

        List<Document> documents = extractDocuments(resource, attach.getContentType());
        if (documents == null) {
            return "지원하지 않는 파일 형식입니다.";
        }

        for (Document doc : documents) {
            doc.getMetadata().put("type", type);
        }

        TokenTextSplitter splitter = new TokenTextSplitter();
        documents = splitter.apply(documents);

        vectorStore.add(documents);

        return String.format("문서 업로드 완료 (%d개 청크)", documents.size());
    }

    private List<Document> extractDocuments(Resource resource, String contentType) {
        try {
            if (contentType.equals("text/plain")) {
                return new TextReader(resource).read();
            } else if (contentType.equals("application/pdf")) {
                return new PagePdfDocumentReader(resource).read();
            } else if (contentType.contains("wordprocessingml")) {
                return new TikaDocumentReader(resource).read();
            }
        } catch (Exception e) {
            log.error("문서 추출 실패", e);
        }
        return null;
    }

    // ===== 벡터 저장소 초기화 =====
    public void clearVectorStore() {
        jdbcTemplate.update("TRUNCATE TABLE vector_store");
    }

    // ===== 디바이스 상태 (정적 메서드 사용) =====
    public Map<String, Object> getDeviceStatus() {
        return Ai2IotTools.getDeviceStatus();
    }

    public void updateDeviceStatus(String device, boolean status) {
        Ai2IotTools.updateDeviceStatus(device, status);
    }

    // ===== STT =====
    private String stt(MultipartFile file) throws IOException {
        Path tempFile = Files.createTempFile("audio-", file.getOriginalFilename());
        file.transferTo(tempFile);
        Resource audioResource = new FileSystemResource(tempFile);

        OpenAiAudioTranscriptionOptions options = OpenAiAudioTranscriptionOptions.builder()
                .model("whisper-1")
                .language("ko")
                .build();

        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioResource, options);
        AudioTranscriptionResponse response = transcriptionModel.call(prompt);

        return response.getResult().getOutput();
    }

    // ===== TTS =====
    private byte[] tts(String text) {
        OpenAiAudioSpeechOptions options = OpenAiAudioSpeechOptions.builder()
                .model("gpt-4o-mini-tts")
                .voice(OpenAiAudioApi.SpeechRequest.Voice.ALLOY)
                .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
                .speed(1.0f)
                .build();

        SpeechPrompt prompt = new SpeechPrompt(text, options);
        SpeechResponse response = speechModel.call(prompt);

        return response.getResult().getOutput();
    }
}