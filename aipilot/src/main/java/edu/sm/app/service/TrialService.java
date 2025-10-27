package edu.sm.app.service;

import edu.sm.app.tool.TrialRoleTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 모의 법정 서비스 - Phase 2 완성본
 *
 * 기능:
 * - Memory: 재판 과정 기록
 * - RAG: 법률 자문
 * - Function Calling: AI 판사/검사/변호사 역할
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TrialService {

    private final ChatClient.Builder chatClientBuilder;
    private final TrialRoleTools trialRoleTools;

    @Autowired
    private ChatMemory chatMemory;

    @Autowired
    private VectorStore vectorStore;

    private static final RoleInstruction DEFAULT_ROLE = new RoleInstruction(
            "참가자",
            "사용자는 특정 역할을 지정하지 않았습니다. 일반적인 재판 참여자로 간주합니다.",
            "AI는 중립적인 진행자로서 요점을 정리하고 필요한 절차나 다음 행동을 안내하세요.",
            "참가자님"
    );

    private static final Map<String, RoleInstruction> ROLE_INSTRUCTIONS = Map.ofEntries(
            Map.entry("judge", new RoleInstruction(
                    "판사",
                    "사용자는 재판을 주도하며 절차를 관리하는 판사입니다.",
                    "AI는 공동 재판부의 시각에서 절차적 유의점과 다음 진행 방향을 제안하고, 다른 참가자의 발언을 어떻게 이끌지 조언하세요.",
                    "판사님"
            )),
            Map.entry("prosecutor", new RoleInstruction(
                    "검사",
                    "사용자는 피고인의 혐의를 입증하려는 검사입니다.",
                    "AI는 재판장의 시각에서 증거 보완, 추가 입증이 필요한 부분, 예상되는 반박을 안내하고 공정한 절차를 강조하세요.",
                    "검사님"
            )),
            Map.entry("defender", new RoleInstruction(
                    "변호인",
                    "사용자는 피고인을 대리해 반론을 제기하는 변호인입니다.",
                    "AI는 판사의 관점에서 주장 구조화를 돕고, 추가로 확인해야 할 사실이나 법리를 제시하며, 균형 잡힌 시각을 유지하세요.",
                    "변호인님"
            )),
            Map.entry("defendant", new RoleInstruction(
                    "피고인",
                    "사용자는 직접 진술하는 피고인입니다.",
                    "AI는 판사로서 권리 고지, 진술 시 주의사항, 정황을 명확히 하기 위한 질문을 제시하며 차분히 안내하세요.",
                    "피고인님"
            )),
            Map.entry("witness", new RoleInstruction(
                    "증인",
                    "사용자는 사실관계를 진술하는 증인입니다.",
                    "AI는 재판부로서 증언의 핵심을 정리하고 추가 확인이 필요한 사실을 질문하며, 선서나 증언 태도에 대해 상기시키세요.",
                    "증인님"
            )),
            Map.entry("jury", new RoleInstruction(
                    "참심위원",
                    "사용자는 배심 또는 시민참여 재판의 참심위원입니다.",
                    "AI는 재판장 시각에서 고려해야 할 판단 요소를 안내하고, 토론을 위한 질문이나 정리 포인트를 제안하세요.",
                    "참심위원님"
            ))
    );

    /**
     * 기본 AI 판사 채팅 (기존 메서드 유지)
     */
    public Flux<String> chat(String message) {
        return chat(message, null);
    }

    /**
     * 역할 맥락을 포함한 AI 판사 채팅
     */
    public Flux<String> chat(String message, String roleId) {
        log.info("사용자 메시지: {}, 역할: {}", message, roleId);

        ChatClient chatClient = chatClientBuilder.build();
        RoleInstruction roleInstruction = resolveRoleInstruction(roleId);
        String systemPrompt = buildSystemPrompt(roleInstruction);
        String userMessage = decorateUserMessage(message, roleInstruction);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .user(message)
                .stream()
                .content();
    }

    /**
     * Memory 기반 재판 진행
     */
    public Flux<String> chatWithMemory(String message, String conversationId) {
        log.info("Memory 채팅 - 대화ID: {}, 메시지: {}", conversationId, message);

        ChatClient chatClient = chatClientBuilder
                .defaultAdvisors(
                        PromptChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1)
                )
                .build();

        return chatClient.prompt()
                .system("""
                    당신은 대한민국 법정의 AI 판사입니다.
                    
                    역할:
                    - 이전 대화를 기억하며 재판을 진행합니다
                    - 피고인의 진술을 경청하고 적절히 질문합니다
                    - 증거와 진술을 종합하여 판단합니다
                    
                    재판 절차:
                    1. 개정 선언
                    2. 혐의 고지
                    3. 피고인 진술 청취
                    4. 증거 확인
                    5. 최종 판결
                    
                    말투:
                    - 판사답게 엄숙하되 친절하게
                    - "~합니다" 형식 사용
                    """)
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

    /**
     * RAG 기반 법률 자문
     */
    public Flux<String> getLegalAdvice(String question, String lawType) {
        log.info("법률 자문 - 유형: {}, 질문: {}", lawType, question);

        SearchRequest.Builder searchRequestBuilder = SearchRequest.builder()
                .similarityThreshold(0.0)
                .topK(3);

        if (StringUtils.hasText(lawType)) {
            searchRequestBuilder.filterExpression("type == '%s'".formatted(lawType));
        }

        SearchRequest searchRequest = searchRequestBuilder.build();

        QuestionAnswerAdvisor questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .build();

        ChatClient chatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1))
                .build();

        return chatClient.prompt()
                .system("""
                    당신은 법률 전문 AI 자문관입니다.
                    
                    역할:
                    - 제공된 법조문을 바탕으로 정확하게 답변합니다
                    - 법률 용어를 쉽게 풀어서 설명합니다
                    - 관련 조문을 명시합니다
                    
                    주의사항:
                    - 문서에 없는 내용은 "관련 법조문을 찾을 수 없습니다"라고 답합니다
                    """)
                .user(question)
                .advisors(questionAnswerAdvisor)
                .stream()
                .content();
    }

    /**
     * Memory + RAG 통합 재판
     */
    public Flux<String> chatWithMemoryAndRag(String message, String conversationId, String lawType) {
        log.info("Memory+RAG 재판 - 대화ID: {}, 법률유형: {}, 메시지: {}", conversationId, lawType, message);

        SearchRequest.Builder searchRequestBuilder = SearchRequest.builder()
                .similarityThreshold(0.0)
                .topK(3);

        if (StringUtils.hasText(lawType)) {
            searchRequestBuilder.filterExpression("type == '%s'".formatted(lawType));
        }

        SearchRequest searchRequest = searchRequestBuilder.build();

        QuestionAnswerAdvisor ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .build();

        ChatClient chatClient = chatClientBuilder
                .defaultAdvisors(
                        PromptChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1)
                )
                .build();

        return chatClient.prompt()
                .system("""
                    당신은 대한민국 법정의 AI 판사입니다.
                    
                    역할:
                    - 이전 대화를 기억하며 재판을 진행합니다
                    - 관련 법조문을 참고하여 판단합니다
                    - 피고인의 진술과 증거를 종합합니다
                    
                    재판 진행:
                    1. 피고인의 진술을 경청합니다
                    2. 관련 법률을 검토합니다
                    3. 정상참작 사유를 고려합니다
                    4. 공정하게 판결합니다
                    """)
                .user(message)
                .advisors(ragAdvisor)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

    private RoleInstruction resolveRoleInstruction(String roleId) {
        if (!StringUtils.hasText(roleId)) {
            return DEFAULT_ROLE;
        }
        return ROLE_INSTRUCTIONS.getOrDefault(roleId, DEFAULT_ROLE);
    }

    private String buildSystemPrompt(RoleInstruction roleInstruction) {
        return String.format("""
                당신은 대한민국 법정의 AI 재판부입니다.

                기본 원칙:
                - 정중하고 공정하게 대화합니다
                - 법률 용어를 쉽게 설명합니다
                - 재판 절차를 안내합니다
                - "~합니다" 형식의 존댓말을 사용합니다

                현재 발언자는 %s 역할입니다.
                역할 설명:
                %s

                응답 지침:
                %s

                답변 시에는 "%s"이라는 호칭으로 부르고,
                발언자의 목적을 존중하면서 필요한 후속 질문이나 다음 절차를 제안하세요.
                """,
                roleInstruction.roleName(),
                roleInstruction.roleSummary(),
                roleInstruction.aiGuide(),
                roleInstruction.salutation()
        );
    }

    private String decorateUserMessage(String message, RoleInstruction roleInstruction) {
        String safeMessage = StringUtils.hasText(message) ? message : "";
        return "[" + roleInstruction.roleName() + "] " + safeMessage;
    }

    /**
     * ⭐ Phase 2 신규: Function Calling 통합 재판
     * AI가 상황에 따라 판사/검사/변호사 함수를 자동 호출
     */
    public Flux<String> conductTrialWithFunctions(String message, String conversationId, String caseInfo) {
        log.info("Function Calling 재판 - 대화ID: {}, 메시지: {}", conversationId, message);

        ChatClient chatClient = chatClientBuilder
                .defaultAdvisors(
                        PromptChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1)
                )
                .defaultTools(trialRoleTools)  // ✅ 이렇게 변경
                .build();

        return chatClient.prompt()
                .system(String.format("""
                    당신은 대한민국 법정의 AI 판사입니다.
                    
                    현재 사건 정보:
                    %s
                    
                    재판 진행:
                    1. 재판 시작 시 judgeOpenTrial() 함수를 호출하여 개정 선언
                    2. 검사가 기소할 때 prosecutorProsecute() 함수 호출
                    3. 변호사가 변론할 때 attorneyDefend() 함수 호출
                    4. 최종 판결 시 judgeVerdict() 함수 호출
                    
                    상황에 맞게 적절한 함수를 호출하세요.
                    함수 호출 결과를 바탕으로 자연스럽게 대화를 진행하세요.
                    """, caseInfo))
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

    private record RoleInstruction(String roleName, String roleSummary, String aiGuide, String salutation) {
    }
}
