package edu.sm.controller;

import edu.sm.app.service.Ai2IntegratedService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/ai2/api")
@Slf4j
@RequiredArgsConstructor
public class Ai2RestController {

    private final Ai2IntegratedService ai2IntegratedService;

    @GetMapping("/sensor-data")
    public Map<String, Object> getSensorData() {
        return ai2IntegratedService.getSensorData();
    }

    @PostMapping("/voice-control")
    public Map<String, String> voiceControl(
            @RequestParam("speech") MultipartFile speech,
            HttpSession session) throws IOException {
        log.info("음성 명령 처리");
        return ai2IntegratedService.processVoiceCommand(speech, session.getId());
    }

    @PostMapping("/text-control")
    public Map<String, String> textControl(
            @RequestParam("command") String command,
            HttpSession session) {
        log.info("텍스트 명령: {}", command);
        return ai2IntegratedService.processTextCommand(command, session.getId());
    }

    @PostMapping("/rag-search")
    public Map<String, String> ragSearch(@RequestParam("question") String question) {
        log.info("RAG 검색: {}", question);
        String answer = ai2IntegratedService.searchManual(question);
        return Map.of("answer", answer);
    }

    @GetMapping(value = "/memory-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> memoryChat(
            @RequestParam("message") String message,
            HttpSession session) {
        log.info("Memory 채팅: {}", message);
        return ai2IntegratedService.chatWithMemory(message, session.getId());
    }

    @PostMapping("/upload-document")
    public String uploadDocument(
            @RequestParam("attach") MultipartFile attach,
            @RequestParam("type") String type) throws IOException {
        log.info("문서 업로드: {}", attach.getOriginalFilename());
        return ai2IntegratedService.uploadDocument(attach, type);
    }

    @PostMapping("/clear-vector")
    public String clearVector() {
        ai2IntegratedService.clearVectorStore();
        return "벡터 저장소를 초기화했습니다.";
    }

    @GetMapping("/device-status")
    public Map<String, Object> getDeviceStatus() {
        return ai2IntegratedService.getDeviceStatus();
    }
}