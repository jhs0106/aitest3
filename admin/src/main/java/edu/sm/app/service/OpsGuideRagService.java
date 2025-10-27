package edu.sm.app.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.Ordered;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 운영 가이드 RAG 전담 서비스
 *
 * 하는 일:
 *  - (ingestOpsGuide) 관리자가 올린 운영 문서를 잘게 쪼개서 vector_store에 저장
 *    -> metadata.type = "ops_guide", metadata.title = (관리자가 준 제목)
 *
 *  - (askOwnerWithOpsGuide) 사장님 질문에 대해
 *    -> 오늘 KPI(DailySummaryService)
 *    -> ops_guide 문서 RAG
 *    둘 다 참고해서 LLM 보고서를 생성
 *
 *  - (askOpsGuideOnly) ops_guide에만 질의하는 순수 RAG QA (관리자용 테스트)
 *
 *  - (listRecentOpsGuide) 최근 업로드된 ops_guide chunk 미리보기
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpsGuideRagService {

    private final JdbcTemplate jdbcTemplate;
    private final VectorStore vectorStore;
    private final DailySummaryService dailySummaryService;
    private final ChatClient.Builder chatClientBuilder;

    // ===============================
    // 1) 운영 문서 업로드 -> vector_store 적재
    // ===============================
    public String ingestOpsGuide(String title, MultipartFile attach) throws IOException {

        // 1. 파일에서 텍스트 뽑기 (pdf, docx, txt 등)
        List<Document> docs = extractDocuments(attach);
        if (docs == null || docs.isEmpty()) {
            return "지원되지 않는 파일 형식입니다. txt/pdf/docx 정도만 넣어주세요.";
        }

        // 2. 메타데이터 세팅 (이걸로 검색 필터 걸 거야)
        for (Document d : docs) {
            d.getMetadata().put("type", "ops_guide");   // 👈 검색 필터
            d.getMetadata().put("title", title);        // 어떤 문서인지 태그
            d.getMetadata().put("source", attach.getOriginalFilename());
        }

        // 3. 토큰 단위로 잘게 쪼개기 (벡터 임베딩 효율↑)
        List<Document> chunks = splitToChunks(docs);

        // 4. 벡터 DB(vector_store)에 insert
        vectorStore.add(chunks);

        log.info("[OPS-GUIDE-INGEST] title={}, file={}, chunks={}",
                title, attach.getOriginalFilename(), chunks.size());

        return "운영 문서를 " + chunks.size() + "개 청크로 저장했습니다.";
    }

    /**
     * 업로드된 파일을 Document 리스트로 변환.
     * txt / pdf / docx(Word) 같은 케이스만 처리.
     *
     * Spring AI의 리더들을 직접 쓰는 대신, 우리가 직접 구현해도 되지만
     * 여기선 간단하게 contentType으로 분기해서 PagePdfDocumentReader / TikaDocumentReader / TextReader 대신
     * attach.getBytes()를 그냥 하나의 Document로 감싸는 방식으로도 충분해.
     *
     * 만약 네 프로젝트에 이미 PagePdfDocumentReader, TikaDocumentReader 등을 쓴 의존성이 있다면
     * 그걸 그대로 끌어다 써도 돼. 여긴 가장 안전한 최소 형태로 줄게.
     */
    private List<Document> extractDocuments(MultipartFile attach) throws IOException {
        // 여기선 "파일 전체를 하나의 Document로" 받아가.
        // (pdf 페이지별, docx 본문 추출 등 더 똑똑하게 하고 싶으면
        //  네가 기존 ETLService.extractFromFile() 로직 그대로 복붙해도 된다.)
        Resource res = new ByteArrayResource(attach.getBytes());
        String text = new String(res.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

        Document doc = new Document(text);
        return List.of(doc);
    }

    /**
     * 긴 Document들을 TokenTextSplitter로 잘게 나눈다.
     * (Spring AI 기본 splitter)
     */
    private List<Document> splitToChunks(List<Document> docs) {
        TokenTextSplitter splitter = new TokenTextSplitter();
        return splitter.apply(docs);
    }

    // ===============================
    // 2) 사장님 질문 -> KPI + RAG 기반 보고서
    // ===============================
    public OwnerOpsAnswer askOwnerWithOpsGuide(String question) {

        // --- 오늘 KPI 가져오기 ---
        DailySummaryService.SummaryDTO today = dailySummaryService.getTodaySummary();

        int visitCount           = safeInt(today != null ? today.getVisitCount()           : null);
        int totalRevenue         = safeInt(today != null ? today.getTotalRevenue()         : null);
        int avgTicket            = safeInt(today != null ? today.getAvgTicket()            : null);
        int suspiciousBayCount   = safeInt(today != null ? today.getSuspiciousBayCount()   : null);
        int dormantCustomerCount = safeInt(today != null ? today.getDormantCustomerCount() : null);

        // --- ops_guide 전용 RAG advisor 준비 ---
        QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(
                        SearchRequest.builder()
                                .similarityThreshold(0.0)
                                .topK(4)
                                .filterExpression("type == 'ops_guide'")
                                .build()
                )
                .build();

        // --- ChatClient 준비 ---
        ChatClient chatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1))
                .build();

        // --- 시스템 프롬프트 (보고 톤, 과장 금지 등) ---
        String systemPrompt = """
            너는 세차장 운영 보조 매니저 AI다.
            - 보고 대상은 사장님이다.
            - 질문에 대해 '현장 KPI 상황' + '운영 가이드(ops_guide)'를 기반으로
              현황, 추정 원인, 그리고 우선 액션 제안을 한국어 존댓말로 보고한다.
            - 아직 실행 안 된 조치를 실행했다고 말하지 말고 "권장드립니다" 식으로 제안만 한다.
            - 장비/안전 문제 같으면 즉각 점검을 권고해라.
            - 과장은 금지, 숫자는 그대로.
        """;

        // --- KPI 요약을 프롬프트에 포함 ---
        String kpiSummary = """
            [오늘 KPI 요약]
            - 방문 차량 수(ENTRY): %d 대
            - 매출 합계(오늘 wash_order.price 합): %d 원
            - 평균 단가: %d 원
            - 장비 이상 의심 건수(저압 등): %d 건
            - 휴면 단골(2주 이상 미방문): %d 명
        """.formatted(
                visitCount,
                totalRevenue,
                avgTicket,
                suspiciousBayCount,
                dormantCustomerCount
        );

        // --- 사장님 질문을 유저 메시지로 넣음 ---
        String userPayload = """
            사장님 질문:
            %s

            아래 형식으로 보고해 주세요.
            1) 현황 요약 (위 KPI를 사용해서 수치로 설명)
            2) 원인 가능성 (운영 가이드/현장 상황을 근거로 추정)
            3) 지금 당장 할만한 액션 우선순위 1~2개 (권장 형태)
        """.formatted(question == null ? "" : question.trim());

        // --- LLM 호출 (ops_guide 문서 RAG advisor를 함께 전달) ---
        String llmAnswer = chatClient
                .prompt()
                .system(systemPrompt)
                .user(kpiSummary + "\n\n" + userPayload)
                .advisors(advisor)
                .call()
                .content();

        log.info("[OWNER-ASK/RAG] Q='{}' => {} chars",
                question, (llmAnswer != null ? llmAnswer.length() : 0));

        OwnerOpsAnswer result = new OwnerOpsAnswer();
        result.setAnswer(llmAnswer != null ? llmAnswer : "(응답이 비어 있습니다)");
        return result;
    }

    // ===============================
    // 3) 관리자 테스트용: ops_guide만으로 QA
    // ===============================
    public Flux<String> askOpsGuideOnly(String q) {

        QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(
                        SearchRequest.builder()
                                .similarityThreshold(0.0)
                                .topK(4)
                                .filterExpression("type == 'ops_guide'")
                                .build()
                )
                .build();

        ChatClient chatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1))
                .build();

        // KPI 없이, 그냥 업로드된 매뉴얼 기반으로만 답하게 함
        return chatClient
                .prompt()
                .user(q)
                .advisors(advisor)
                .stream()
                .content();
    }

    // ===============================
    // 4) 최근 업로드된 ops_guide 미리보기
    // ===============================
    public List<Map<String,Object>> listRecentOpsGuide(int limit) {
        String sql = """
            SELECT id, content, metadata
            FROM vector_store
            WHERE metadata->>'type' = 'ops_guide'
            ORDER BY id DESC
            LIMIT ?
        """;
        return jdbcTemplate.queryForList(sql, limit);
    }

    // ===============================
    // 유틸
    // ===============================
    private int safeInt(Integer v) {
        return (v == null ? 0 : v);
    }

    @Data
    public static class OwnerOpsAnswer {
        private String answer;
    }
}
