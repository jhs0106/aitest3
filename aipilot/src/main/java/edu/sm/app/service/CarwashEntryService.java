package edu.sm.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import edu.sm.app.tool.VehicleCheckTools;
import edu.sm.app.tool.CarwashTools;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarwashEntryService {

    private final ChatModel chatModel;
    private final VehicleCheckTools vehicleCheckTools;
    private final CarwashTools carwashTools;
    private final CarwashPlanService carwashPlanService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EntryResult handleEntry(String contentType, byte[] bytes) throws Exception {

        Media media = Media.builder()
                .mimeType(MimeType.valueOf(contentType))
                .data(new ByteArrayResource(bytes))
                .build();

        UserMessage userMessage = UserMessage.builder()
                .text("""
                이미지에서 차량 번호판을 읽어라.
                번호판 형태는 '숫자2~3자리 + 한글1글자 + 숫자4자리' 예) '12가3456'.
                
                1) 추출한 번호판을 checkPlateRegistration 도구로 확인해라.
                2) 등록된 차량이면 gateOpen 도구를 호출하라.
                3) 등록되지 않은 차량이면 gateClose 도구를 호출하라.
                
                마지막 답변은 JSON만 반환:
                {"plate":"12가3456","registered":true}
                """)
                .media(media)
                .build();

        ChatClient chatClient = ChatClient.builder(chatModel).build();
        String llmResp = chatClient
                .prompt()
                .messages(userMessage)
                .tools(vehicleCheckTools, carwashTools)
                .call()
                .content();

        log.info("[ENTRY] llmResponse raw={}", llmResp);

        String cleaned = llmResp.replace("```json","")
                .replace("```","")
                .trim();

        JsonNode node = objectMapper.readTree(cleaned);
        String plate = node.path("plate").asText("").replaceAll("\\s+","");
        boolean registered = node.path("registered").asBoolean(false);

        // 이 시점에서 vehicle 테이블에 plate가 없다면 insert 되고,
        // 있으면 그대로 사용. existedBefore=true/false 알려줌
        boolean existedBefore = carwashPlanService.ensureVehicleProfileEmbedding(plate);

        EntryResult r = new EntryResult();
        r.setPlate(plate);
        // knownCustomer는 기존에 있던 row가 있었는지로 판단 (또는 LLM의 registered도 참고)
        r.setKnownCustomer(existedBefore || registered);
        r.setGate(registered ? "OPENED" : "CLOSED");

        return r;
    }

    @lombok.Data
    public static class EntryResult {
        private String plate;
        private boolean knownCustomer;
        private String gate;
    }
}
