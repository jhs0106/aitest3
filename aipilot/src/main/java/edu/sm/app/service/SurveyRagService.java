package edu.sm.app.service;

import edu.sm.app.dto.CounselingRequest;
import edu.sm.app.dto.CounselingResponse;
import edu.sm.app.dto.SelfAssessmentForm;
import edu.sm.app.dto.SelfAssessmentOption;
import edu.sm.app.dto.SelfAssessmentQuestion;
import edu.sm.app.dto.SurveyAnswerRecord;
import edu.sm.app.dto.SurveyAnswerSubmission;
import edu.sm.app.dto.SurveyRecord;
import edu.sm.app.dto.SurveySubmission;
import edu.sm.app.tool.SurveyRagTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 설문 수집/저장 및 RAG(검색기반) 상담 생성 서비스.
 * 유틸/정적 도메인 로직은 SurveyRagTools 로 위임하여 테스트/재사용/가독성을 개선.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SurveyRagService {

    // ====== 상수 ======
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /** 설문 폼 카탈로그: SurveyRagTools 에서 생성/관리 */
    private static final Map<String, SelfAssessmentForm> FORM_CATALOG = SurveyRagTools.createFormCatalog();

    // ====== 상태 저장소 & DI 의존성 ======
    /** 클라이언트별 설문 기록 인메모리 저장소 */
    private final Map<String, List<SurveyRecord>> surveyStore = new ConcurrentHashMap<>();
    private final VectorStore vectorStore;
    private final ChatClient.Builder chatClientBuilder;

    // ====== 조회 API ======
    /**
     * 설문 폼 리스트 조회 (카테고리 지정 시 해당 폼만 반환)
     */
    public List<SelfAssessmentForm> getForms(Optional<String> categoryOptional) {
        if (categoryOptional.isPresent()) {
            String category = SurveyRagTools.normalize(categoryOptional.get());
            if (category == null) {
                return List.of();
            }
            SelfAssessmentForm form = FORM_CATALOG.get(category);
            return form == null ? List.of() : List.of(form);
        }
        return FORM_CATALOG.values().stream()
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    /**
     * 설문 질문 리스트 조회 (카테고리 지정 시 해당 폼의 문항만)
     */
    public List<SelfAssessmentQuestion> getQuestions(Optional<String> categoryOptional) {
        return getForms(categoryOptional).stream()
                .flatMap(form -> form.getQuestions().stream())
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    // ====== 저장 API ======
    /**
     * 설문 제출 저장 + VectorStore 색인
     * @return 생성된 surveyId
     */
    public String storeSurvey(SurveySubmission submission) {
        if (submission == null) {
            throw new IllegalArgumentException("설문 정보가 비어 있습니다.");
        }

        String clientId = SurveyRagTools.normalize(submission.getClientId());
        String category = SurveyRagTools.normalize(submission.getCategory());
        if (clientId == null || category == null) {
            throw new IllegalArgumentException("clientId와 category는 필수 항목입니다.");
        }

        SelfAssessmentForm form = FORM_CATALOG.get(category);
        if (form == null) {
            throw new IllegalArgumentException("지원하지 않는 설문 유형입니다.");
        }

        // 문항/응답 룩업 테이블 구성
        Map<String, SelfAssessmentQuestion> questionLookup = form.getQuestions().stream()
                .collect(Collectors.toMap(
                        SelfAssessmentQuestion::getId,
                        q -> q,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        Map<String, SurveyAnswerSubmission> answerLookup = Optional.ofNullable(submission.getAnswers())
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .filter(a -> a.getQuestionId() != null && a.getSelectedOptionId() != null)
                .collect(Collectors.toMap(
                        SurveyAnswerSubmission::getQuestionId,
                        it -> it,
                        (a, b) -> b,
                        LinkedHashMap::new
                ));

        if (answerLookup.isEmpty()) {
            throw new IllegalArgumentException("모든 문항에 대한 응답을 선택해 주세요.");
        }

        // 채점 및 기록 생성
        List<SurveyAnswerRecord> answerRecords = new ArrayList<>();
        int totalScore = 0;

        for (SelfAssessmentQuestion question : form.getQuestions()) {
            SurveyAnswerSubmission answerSubmission = answerLookup.get(question.getId());
            if (answerSubmission == null) {
                throw new IllegalArgumentException("문항 '" + question.getText() + "'의 응답이 누락되었습니다.");
            }

            SelfAssessmentOption selectedOption = question.getOptions().stream()
                    .filter(opt -> opt.getId().equals(answerSubmission.getSelectedOptionId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("문항 '" + question.getText() + "'의 선택지가 올바르지 않습니다."));

            Integer score = Optional.ofNullable(selectedOption.getScore()).orElse(0);
            answerRecords.add(new SurveyAnswerRecord(
                    question.getId(),
                    question.getText(),
                    selectedOption.getId(),
                    selectedOption.getText(),
                    score
            ));
            totalScore += score;
        }

        // 결과 생성
        String surveyId = UUID.randomUUID().toString();
        String submittedAt = ZonedDateTime.now(ZoneOffset.UTC).format(DATE_TIME_FORMATTER);
        String selfReflection = SurveyRagTools.normalize(submission.getSelfReflection());

        int maxScore = form.getMaxScore();
        var guide = SurveyRagTools.resolveGuide(form, totalScore).orElse(null);

        String resultLevel = guide != null ? guide.getLevel() : null;
        String resultSummary = guide != null
                ? guide.getSummary()
                : SurveyRagTools.summarizeResult(category, totalScore, maxScore);
        String resultRecommendation = guide != null
                ? guide.getRecommendation()
                : SurveyRagTools.recommendNextStep(category, totalScore, maxScore);

        SurveyRecord surveyRecord = new SurveyRecord(
                surveyId,
                clientId,
                category,
                List.copyOf(answerRecords),
                totalScore,
                maxScore,
                resultLevel,
                resultSummary,
                resultRecommendation,
                selfReflection,
                submittedAt
        );

        // 저장 & 색인
        surveyStore.computeIfAbsent(clientId, k -> new CopyOnWriteArrayList<>()).add(surveyRecord);
        indexSurveyRecord(surveyRecord, form);

        return surveyId;
    }

    // ====== 조회(저장 이후) ======
    /**
     * 특정 clientId (및 선택적 category)로 설문 기록 조회 (최신순 정렬)
     */
    public List<SurveyRecord> findSurveys(String clientId, Optional<String> category) {
        String normalizedClientId = SurveyRagTools.normalize(clientId);
        if (normalizedClientId == null) {
            return List.of();
        }

        List<SurveyRecord> surveys = surveyStore.getOrDefault(normalizedClientId, List.of());

        Optional<String> normalizedCategory = category
                .flatMap(value -> Optional.ofNullable(SurveyRagTools.normalize(value)));

        List<SurveyRecord> filtered = surveys.stream()
                .filter(record -> normalizedCategory
                        .map(cat -> cat.equalsIgnoreCase(SurveyRagTools.normalize(record.getCategory())))
                        .orElse(true))
                .sorted(Comparator.comparing(this::parseSubmittedAt).reversed())
                .collect(Collectors.toCollection(ArrayList::new));

        return Collections.unmodifiableList(filtered);
    }

    // ====== 상담 생성(RAG) ======
    /**
     * 사용자의 질문 + (선택적) 카테고리 + 사용자의 최근 설문을 바탕으로
     * VectorStore를 조회하고 조언(상담)을 생성
     */
    public CounselingResponse generateCounseling(CounselingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("상담 요청이 비어 있습니다.");
        }

        String clientId = SurveyRagTools.normalize(request.getClientId());
        String question = SurveyRagTools.normalize(request.getQuestion());
        if (clientId == null || question == null) {
            throw new IllegalArgumentException("clientId와 question은 필수 항목입니다.");
        }

        Optional<String> category = Optional.ofNullable(SurveyRagTools.normalize(request.getCategory()));
        List<SurveyRecord> references = findSurveys(clientId, category);

        // VectorStore 필터 구성
        String filterExpression = SurveyRagTools.buildFilterExpression(clientId, category);
        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .similarityThreshold(0.0)
                .topK(5)
                .filterExpression(filterExpression)
                .build();

        QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .build();

        ChatClient chatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1))
                .build();

        String userPrompt = SurveyRagTools.buildUserPrompt(question, clientId, category, references);

        String advice = chatClient
                .prompt()
                .system("""
                        너는 마음건강 상담 전문가다.
                        제공되는 설문 요약과 사용자의 질문을 활용해 공감적이고 실천 가능한 조언을 한국어로 작성하라.
                        설문에서 확인되지 않는 내용은 추측하지 말고, 필요한 경우 전문기관이나 추가 상담을 안내하라.
                        """)
                .user(userPrompt)
                .advisors(advisor)
                .call()
                .content();

        if (advice == null || advice.isBlank()) {
            advice = SurveyRagTools.buildFallbackAdvice(question, references);
        }

        return new CounselingResponse(advice, references.isEmpty() ? null : references);
    }

    // ====== 내부 유틸 ======
    /** 설문 문서를 VectorStore 에 색인 */
    private void indexSurveyRecord(SurveyRecord record, SelfAssessmentForm form) {
        try {
            Document document = toDocument(record, form);
            vectorStore.add(List.of(document));
        } catch (Exception ex) {
            log.warn("설문 벡터 적재 실패 - surveyId={}, reason={}", record.getSurveyId(), ex.getMessage());
        }
    }

    /** SurveyRecord -> Document 변환 (메타데이터 포함) */
    private Document toDocument(SurveyRecord record, SelfAssessmentForm form) {
        StringBuilder content = new StringBuilder();
        content.append("설문 요약\n");
        content.append("clientId: ").append(record.getClientId()).append('\n');
        content.append("category: ").append(record.getCategory()).append('\n');
        if (record.getResultLevel() != null) {
            content.append("level: ").append(record.getResultLevel()).append('\n');
        }
        if (record.getResultSummary() != null) {
            content.append("summary: ").append(record.getResultSummary()).append('\n');
        }
        if (record.getResultRecommendation() != null) {
            content.append("recommendation: ").append(record.getResultRecommendation()).append('\n');
        }
        if (record.getAnswers() != null && !record.getAnswers().isEmpty()) {
            content.append("answers:\n");
            for (SurveyAnswerRecord a : record.getAnswers()) {
                content.append("- Q(").append(a.getQuestionId()).append(") ")
                        .append(a.getQuestionText()).append(" -> [")
                        .append(a.getSelectedOptionId()).append("] ")
                        .append(a.getSelectedOptionText()).append(" (score=")
                        .append(Optional.ofNullable(a.getScore()).orElse(0))
                        .append(")\n");
            }
        }
        if (record.getSelfReflection() != null) {
            content.append("selfReflection: ").append(record.getSelfReflection()).append('\n');
        }
        content.append("totalScore: ").append(record.getTotalScore()).append('/')
                .append(record.getMaxScore()).append('\n');
        if (record.getSubmittedAt() != null) {
            content.append("submittedAt: ").append(record.getSubmittedAt()).append('\n');
        }

        if (form != null) {
            content.append("formTitle: ").append(form.getTitle()).append('\n');
            if (form.getDescription() != null) {
                content.append("formDescription: ").append(form.getDescription()).append('\n');
            }
        }

        Document document = new Document(content.toString());
        document.getMetadata().put("type", "survey_record");
        document.getMetadata().put("surveyId", record.getSurveyId());
        document.getMetadata().put("clientId", record.getClientId());
        if (record.getCategory() != null) {
            document.getMetadata().put("category", record.getCategory());
        }
        if (record.getSubmittedAt() != null) {
            document.getMetadata().put("submittedAt", record.getSubmittedAt());
        }
        if (form != null && form.getTitle() != null) {
            document.getMetadata().put("formTitle", form.getTitle());
        }
        return document;
    }


    private ZonedDateTime parseSubmittedAt(SurveyRecord record) {
        if (record.getSubmittedAt() == null) {
            return ZonedDateTime.now(ZoneOffset.UTC);
        }
        return ZonedDateTime.parse(record.getSubmittedAt(), DATE_TIME_FORMATTER);
    }
}
