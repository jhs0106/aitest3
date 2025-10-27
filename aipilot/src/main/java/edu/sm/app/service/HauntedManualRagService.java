package edu.sm.app.service;


import edu.sm.app.dto.ManualAnswerResponse;
import edu.sm.app.dto.ManualStartResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class HauntedManualRagService {

    private final HauntedManualEtlService etlService;
    private final ChatClient.Builder chatClientBuilder;
    private final SimpleLoggerAdvisor loggerAdvisor;

    public HauntedManualRagService(HauntedManualEtlService etlService,
                                   ChatMemory chatMemory,
                                   ChatClient.Builder chatClientBuilder) {
        this.etlService = etlService;
        this.loggerAdvisor = new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1);
        PromptChatMemoryAdvisor chatMemoryAdvisor = PromptChatMemoryAdvisor.builder(chatMemory).build();
        this.chatClientBuilder = chatClientBuilder
                .defaultAdvisors(chatMemoryAdvisor, this.loggerAdvisor);
    }

    public ManualStartResponse startScenario(String scenario,
                                             String rumor) {
        String trimmedScenario = StringUtils.hasText(scenario) ? StringUtils.trimWhitespace(scenario) : "폐교";
        String trimmedRumor = StringUtils.hasText(rumor) ? StringUtils.trimWhitespace(rumor) : "";

        String conversationId = generateConversationId(trimmedScenario);
        String prompt = buildManualPrimer(trimmedScenario, trimmedRumor);
        QuestionAnswerAdvisor advisor = etlService.createAdvisor(trimmedScenario);
        String manual = callModel(prompt, conversationId, advisor);

        return new ManualStartResponse(conversationId, trimmedScenario, manual);
    }

    public ManualAnswerResponse answerManual(String conversationId,
                                             String question,
                                             String scenario,
                                             String rumor) {
        if (!StringUtils.hasText(conversationId)) {
            throw new IllegalArgumentException("conversationId가 필요합니다. 시나리오를 다시 시작하세요.");
        }
        if (!StringUtils.hasText(question)) {
            throw new IllegalArgumentException("질문이 비어 있습니다.");
        }
        String trimmedScenario = StringUtils.hasText(scenario) ? StringUtils.trimWhitespace(scenario) : "폐교";
        String trimmedRumor = StringUtils.hasText(rumor) ? StringUtils.trimWhitespace(rumor) : "";

        String prompt = buildFollowUpPrompt(trimmedScenario, trimmedRumor, question);
        QuestionAnswerAdvisor advisor = etlService.createAdvisor(trimmedScenario);
        String answer = callModel(prompt, conversationId, advisor);

        return new ManualAnswerResponse(answer);
    }

    private String callModel(String prompt,
                             String conversationId,
                             QuestionAnswerAdvisor advisor) {
        ChatClient chatClient = chatClientBuilder.build();
        return chatClient.prompt()
                .user(prompt)
                .advisors(advisor)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }

    private String buildManualPrimer(String scenario,
                                     String rumor) {
        StringBuilder builder = new StringBuilder();
        builder.append("당신은 ")
                .append(scenario)
                .append(" 야간 근무자의 규칙 보관 담당자입니다.\n")
                .append("업무 시작을 위해 현 시점에서 반드시 지켜야 할 괴담형 매뉴얼을 작성하세요.\n")
                .append("1. 규칙 갱신: 3~5개의 핵심 절차를 조항으로 작성\n")
                .append("2. 위반 시나리오: 규칙이 깨지는 순간의 단계적 묘사\n")
                .append("3. 불길한 징후: 감각 중심의 경고\n")
                .append("4. 열린 결말: '결말 미확정' 등으로 마무리\n")
                .append("각 단락 사이에는 — 구분선을 넣으세요.\n")
                .append("새 근무자는 이 매뉴얼을 숙지하고 질문을 시작합니다.");
        if (StringUtils.hasText(rumor)) {
            builder.append("\n최신 소문: ").append(rumor)
                    .append("\n(소문은 사실이 아닐 수 있음을 상기시키세요.)");
        }
        return builder.toString();
    }

    private String buildFollowUpPrompt(String scenario,
                                       String rumor,
                                       String question) {
        StringBuilder builder = new StringBuilder();
        builder.append("당신은 ")
                .append(scenario)
                .append(" 야간 근무자의 규칙 보관 담당자입니다.\n")
                .append("답변은 반드시 매뉴얼형 괴담 구조를 따르세요.\n")
                .append("1. 규칙 갱신: 2~4개의 조항으로 현재 규칙을 요약\n")
                .append("2. 위반 시나리오: 규칙이 어겨질 때의 단계 묘사\n")
                .append("3. 불길한 징후: 감각 묘사 중심의 경고\n")
                .append("4. 열린 결말: '결말 미확정' 등으로 마무리\n")
                .append("각 단락 사이에는 — 구분선을 넣으세요.\n")
                .append("사용자 질문: ")
                .append(question);
        if (StringUtils.hasText(rumor)) {
            builder.append("\n최신 소문: ").append(rumor)
                    .append("\n(소문은 사실이 아닐 수 있음을 상기시키세요.)");
        }
        builder.append("\n앞선 매뉴얼 내용을 바탕으로 규칙을 갱신하세요.");
        return builder.toString();
    }

    private String generateConversationId(String scenario) {
        String base = scenario.replaceAll("\\s+", "_");
        return base + "-" + UUID.randomUUID();
    }
}
