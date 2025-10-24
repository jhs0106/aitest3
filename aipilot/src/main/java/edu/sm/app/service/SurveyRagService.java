package edu.sm.app.service;

import edu.sm.app.dto.CounselingRequest;
import edu.sm.app.dto.CounselingResponse;
import edu.sm.app.dto.SelfAssessmentForm;
import edu.sm.app.dto.SelfAssessmentGuide;
import edu.sm.app.dto.SelfAssessmentOption;
import edu.sm.app.dto.SelfAssessmentQuestion;
import edu.sm.app.dto.SurveyAnswerRecord;
import edu.sm.app.dto.SurveyAnswerSubmission;
import edu.sm.app.dto.SurveyRecord;
import edu.sm.app.dto.SurveySubmission;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.Ordered;

@Service
@Slf4j
@RequiredArgsConstructor
public class SurveyRagService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static final Map<String, SelfAssessmentForm> FORM_CATALOG = createFormCatalog();

    private final Map<String, List<SurveyRecord>> surveyStore = new ConcurrentHashMap<>();
    private final VectorStore vectorStore;
    private final ChatClient.Builder chatClientBuilder;

    public List<SelfAssessmentForm> getForms(Optional<String> categoryOptional) {
        if (categoryOptional.isPresent()) {
            String category = normalize(categoryOptional.get());
            if (category == null) {
                return List.of();
            }
            SelfAssessmentForm form = FORM_CATALOG.get(category);
            return form == null ? List.of() : List.of(form);
        }
        return FORM_CATALOG.values().stream()
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    public List<SelfAssessmentQuestion> getQuestions(Optional<String> categoryOptional) {
        return getForms(categoryOptional).stream()
                .flatMap(form -> form.getQuestions().stream())
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    public String storeSurvey(SurveySubmission submission) {
        if (submission == null) {
            throw new IllegalArgumentException("설문 정보가 비어 있습니다.");
        }

        String clientId = normalize(submission.getClientId());
        String category = normalize(submission.getCategory());

        if (clientId == null || category == null) {
            throw new IllegalArgumentException("clientId와 category는 필수 항목입니다.");
        }

        SelfAssessmentForm form = FORM_CATALOG.get(category);
        if (form == null) {
            throw new IllegalArgumentException("지원하지 않는 설문 유형입니다.");
        }

        Map<String, SelfAssessmentQuestion> questionLookup = form.getQuestions().stream()
                .collect(Collectors.toMap(SelfAssessmentQuestion::getId, question -> question, (a, b) -> a, LinkedHashMap::new));

        Map<String, SurveyAnswerSubmission> answerLookup = Optional.ofNullable(submission.getAnswers())
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .filter(answer -> answer.getQuestionId() != null && answer.getSelectedOptionId() != null)
                .collect(Collectors.toMap(SurveyAnswerSubmission::getQuestionId, it -> it, (a, b) -> b, LinkedHashMap::new));

        if (answerLookup.isEmpty()) {
            throw new IllegalArgumentException("모든 문항에 대한 응답을 선택해 주세요.");
        }

        List<SurveyAnswerRecord> answerRecords = new ArrayList<>();
        int totalScore = 0;

        for (SelfAssessmentQuestion question : form.getQuestions()) {
            SurveyAnswerSubmission answerSubmission = answerLookup.get(question.getId());
            if (answerSubmission == null) {
                throw new IllegalArgumentException("문항 '" + question.getText() + "'의 응답이 누락되었습니다.");
            }

            SelfAssessmentOption selectedOption = question.getOptions().stream()
                    .filter(option -> option.getId().equals(answerSubmission.getSelectedOptionId()))
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

        String surveyId = UUID.randomUUID().toString();
        String submittedAt = ZonedDateTime.now(ZoneOffset.UTC).format(DATE_TIME_FORMATTER);
        String selfReflection = normalize(submission.getSelfReflection());

        int maxScore = form.getMaxScore();
        SelfAssessmentGuide guide = resolveGuide(form, totalScore).orElse(null);

        String resultLevel = guide != null ? guide.getLevel() : null;
        String resultSummary = guide != null ? guide.getSummary() : summarizeResult(category, totalScore, maxScore);
        String resultRecommendation = guide != null ? guide.getRecommendation() : recommendNextStep(category, totalScore, maxScore);

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

        surveyStore
                .computeIfAbsent(clientId, key -> new CopyOnWriteArrayList<>())
                .add(surveyRecord);

        indexSurveyRecord(surveyRecord, form);

        return surveyId;
    }

    public List<SurveyRecord> findSurveys(String clientId, Optional<String> category) {
        String normalizedClientId = normalize(clientId);
        if (normalizedClientId == null) {
            return List.of();
        }

        List<SurveyRecord> surveys = surveyStore.getOrDefault(normalizedClientId, List.of());

        Optional<String> normalizedCategory = category
                .flatMap(value -> Optional.ofNullable(normalize(value)));

        List<SurveyRecord> filtered = surveys.stream()
                .filter(record -> normalizedCategory
                        .map(cat -> cat.equalsIgnoreCase(normalize(record.getCategory())))
                        .orElse(true))
                .sorted(Comparator.comparing(this::parseSubmittedAt).reversed())
                .collect(Collectors.toCollection(ArrayList::new));

        return Collections.unmodifiableList(filtered);
    }
    public CounselingResponse generateCounseling(CounselingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("상담 요청이 비어 있습니다.");
        }

        String clientId = normalize(request.getClientId());
        String question = normalize(request.getQuestion());

        if (clientId == null || question == null) {
            throw new IllegalArgumentException("clientId와 question은 필수 항목입니다.");
        }

        Optional<String> category = Optional.ofNullable(normalize(request.getCategory()));
        List<SurveyRecord> references = findSurveys(clientId, category);

        String filterExpression = buildFilterExpression(clientId, category);
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

        String userPrompt = buildUserPrompt(question, clientId, category, references);

        String advice = chatClient.prompt()
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
            advice = buildFallbackAdvice(question, references);
        }

        return new CounselingResponse(advice, references.isEmpty() ? null : references);
    }

    private void indexSurveyRecord(SurveyRecord record, SelfAssessmentForm form) {
        try {
            Document document = toDocument(record, form);
            vectorStore.add(List.of(document));
        } catch (Exception ex) {
            log.warn("설문 벡터 적재 실패 - surveyId={}, reason={}", record.getSurveyId(), ex.getMessage());
        }
    }

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
        if (record.getTotalScore() != null && record.getMaxScore() != null) {
            content.append("score: ").append(record.getTotalScore()).append(" / ").append(record.getMaxScore()).append('\n');
        }
        if (record.getSelfReflection() != null) {
            content.append("selfReflection: ").append(record.getSelfReflection()).append('\n');
        }
        content.append("answers:\n");
        List<SurveyAnswerRecord> answers = record.getAnswers() == null ? List.of() : record.getAnswers();
        for (SurveyAnswerRecord answer : answers) {
            content.append("- ")
                    .append(answer.getQuestionText())
                    .append(" -> ")
                    .append(answer.getSelectedOptionText());
            if (answer.getScore() != null) {
                content.append(" (score=").append(answer.getScore()).append(')');
            }
            content.append('\n');
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

    private String buildFilterExpression(String clientId, Optional<String> categoryOptional) {
        StringBuilder filter = new StringBuilder("type == 'survey_record' && clientId == '")
                .append(escapeQuotes(clientId))
                .append("'");
        categoryOptional.ifPresent(cat -> filter.append(" && category == '").append(escapeQuotes(cat)).append("'"));
        return filter.toString();
    }

    private String buildUserPrompt(String question,
                                   String clientId,
                                   Optional<String> category,
                                   List<SurveyRecord> references) {
        StringBuilder builder = new StringBuilder();
        builder.append("상담 대상 클라이언트: ").append(clientId).append('\n');
        builder.append("질문: ").append(question).append('\n');
        builder.append("카테고리: ").append(category.orElse("미지정")).append('\n');
        if (references.isEmpty()) {
            builder.append("설문 데이터: 최근 설문 기록이 없습니다. 일반적인 조언을 제공하되, 정기적인 자가 검진을 안내하세요.");
            return builder.toString();
        }

        builder.append("최근 설문 기록 요약:\n");
        references.stream()
                .limit(3)
                .forEach(record -> builder.append(formatSurveyRecord(record)).append('\n'));

        builder.append("설문 기반으로 질문에 답변하되, 필요한 후속 조치를 제안하세요.");
        return builder.toString();
    }

    private String formatSurveyRecord(SurveyRecord record) {
        StringBuilder builder = new StringBuilder();
        builder.append("- 제출일: ").append(Optional.ofNullable(record.getSubmittedAt()).orElse("미상")).append('\n');
        if (record.getResultLevel() != null) {
            builder.append("  단계: ").append(record.getResultLevel()).append('\n');
        }
        if (record.getResultSummary() != null) {
            builder.append("  요약: ").append(record.getResultSummary()).append('\n');
        }
        if (record.getResultRecommendation() != null) {
            builder.append("  권장 행동: ").append(record.getResultRecommendation()).append('\n');
        }
        List<SurveyAnswerRecord> answers = record.getAnswers() == null ? List.of() : record.getAnswers();
        if (!answers.isEmpty()) {
            builder.append("  주요 응답: ");
            List<SurveyAnswerRecord> top = answers.stream()
                    .sorted(Comparator.comparingInt((SurveyAnswerRecord a) -> Optional.ofNullable(a.getScore()).orElse(0)).reversed())
                    .limit(2)
                    .collect(Collectors.toList());
            for (int i = 0; i < top.size(); i++) {
                SurveyAnswerRecord answer = top.get(i);
                if (i > 0) {
                    builder.append(i == top.size() - 1 ? " 그리고 " : ", ");
                }
                builder.append("'")
                        .append(answer.getQuestionText())
                        .append("'→'")
                        .append(answer.getSelectedOptionText())
                        .append("'");
            }
            builder.append('\n');
        }
        if (record.getSelfReflection() != null) {
            builder.append("  자가 소감: ").append(record.getSelfReflection()).append('\n');
        }
        return builder.toString();
    }

    private String buildFallbackAdvice(String question, List<SurveyRecord> references) {
        if (references.isEmpty()) {
            return "최근 자가 설문 기록이 없어 일반적인 조언을 제공합니다. 질문하신 내용은 '" +
                    question + "'이며, 정기적으로 자가 테스트를 진행하면 더 맞춤형 안내를 받을 수 있습니다.";
        }
        SurveyRecord latest = references.get(0);
        StringBuilder builder = new StringBuilder();
        builder.append("자가 설문 결과를 참고하여 질문 '")
                .append(question)
                .append("'에 대해 안내드립니다. ");
        if (latest.getResultLevel() != null) {
            builder.append("현재 상태는 '")
                    .append(latest.getResultLevel())
                    .append("' 단계로 평가되었습니다. ");
        }
        if (latest.getResultSummary() != null) {
            builder.append("요약: ")
                    .append(latest.getResultSummary())
                    .append(". ");
        }
        if (latest.getResultRecommendation() != null) {
            builder.append("권장 행동: ")
                    .append(latest.getResultRecommendation())
                    .append(". ");
        }
        builder.append("필요하다면 전문 상담이나 주변의 도움을 요청해 보시길 권장드립니다.");
        return builder.toString();
    }

    private String escapeQuotes(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
    private Optional<SelfAssessmentGuide> resolveGuide(SelfAssessmentForm form, int totalScore) {
        if (form.getGuides() == null) {
            return Optional.empty();
        }
        return form.getGuides().stream()
                .filter(guide -> totalScore >= guide.getMinScore() && totalScore <= guide.getMaxScore())
                .findFirst()
                .or(() -> form.getGuides().isEmpty() ? Optional.empty() : Optional.of(form.getGuides().get(form.getGuides().size() - 1)));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ZonedDateTime parseSubmittedAt(SurveyRecord record) {
        if (record.getSubmittedAt() == null) {
            return ZonedDateTime.now(ZoneOffset.UTC);
        }
        return ZonedDateTime.parse(record.getSubmittedAt(), DATE_TIME_FORMATTER);
    }

    private String summarizeResult(String category, int totalScore, int maxScore) {
        if (maxScore <= 0) {
            return "설문 결과를 해석할 수 없습니다.";
        }
        double ratio = (double) totalScore / maxScore;

        return switch (category) {
            case "psychology" -> {
                if (ratio < 0.25) {
                    yield "현재 정서적 안정을 잘 유지하고 있습니다.";
                } else if (ratio < 0.6) {
                    yield "일상적인 스트레스가 있으나 스스로 조절 가능한 범위로 보입니다.";
                } else {
                    yield "높은 수준의 스트레스 신호가 감지됩니다. 휴식과 주변의 지지가 필요할 수 있습니다.";
                }
            }
            case "career" -> {
                if (ratio < 0.3) {
                    yield "진로에 대해 비교적 명확한 방향성을 갖고 있습니다.";
                } else if (ratio < 0.65) {
                    yield "진로와 관련해 탐색이 더 필요하며, 몇 가지 불확실성이 느껴집니다.";
                } else {
                    yield "진로 스트레스가 높아 구체적인 지원 전략을 세워보는 것이 좋겠습니다.";
                }
            }
            default -> {
                if (ratio < 0.4) {
                    yield "안정적인 상태가 유지되고 있습니다.";
                } else if (ratio < 0.7) {
                    yield "주의가 필요한 변화가 포착되었습니다.";
                } else {
                    yield "심화된 지원이 필요한 신호입니다.";
                }
            }
        };
    }

    private String recommendNextStep(String category, int totalScore, int maxScore) {
        if (maxScore <= 0) {
            return null;
        }
        double ratio = (double) totalScore / maxScore;

        return switch (category) {
            case "psychology" -> {
                if (ratio < 0.25) {
                    yield "지금의 생활 리듬을 유지하면서 가벼운 휴식을 계속 챙겨보세요.";
                } else if (ratio < 0.6) {
                    yield "하루 중 짧은 휴식 시간을 확보하고, 주변과 감정을 나누는 연습을 해보세요.";
                } else {
                    yield "가능하다면 전문 상담 또는 신뢰하는 사람과의 깊은 대화를 통해 도움을 요청해 보세요.";
                }
            }
            case "career" -> {
                if (ratio < 0.3) {
                    yield "계획하고 있는 진로 활동을 꾸준히 이어가며, 강점 점검을 병행해 보세요.";
                } else if (ratio < 0.65) {
                    yield "선호 직무를 더 탐색하고, 필요한 역량을 정리해 학습 계획을 세워보세요.";
                } else {
                    yield "커리어 코칭이나 취업 상담 등 외부 지원을 활용해 구체적인 실행 전략을 세워보세요.";
                }
            }
            default -> {
                if (ratio < 0.4) {
                    yield "현재의 자기 관리 루틴을 유지하세요.";
                } else if (ratio < 0.7) {
                    yield "일상에서 회복을 돕는 활동을 늘려 보세요.";
                } else {
                    yield "주변의 도움을 적극적으로 요청하고 전문가 상담을 고려해 보세요.";
                }
            }
        };
    }

    private static Map<String, SelfAssessmentForm> createFormCatalog() {
        Map<String, SelfAssessmentForm> catalog = new LinkedHashMap<>();

        List<SelfAssessmentQuestion> psychologyQuestions = List.of(
                new SelfAssessmentQuestion(
                        "mood_balance",
                        "psychology",
                        "최근 일주일 동안 전반적인 기분은 어땠나요?",
                        List.of(
                                new SelfAssessmentOption("mood_balance_0", "대체로 평온했고 큰 기복이 없었다", 0),
                                new SelfAssessmentOption("mood_balance_1", "가끔 스트레스를 느꼈지만 스스로 조절했다", 1),
                                new SelfAssessmentOption("mood_balance_2", "스트레스가 누적되었고 쉽게 지쳤다", 3),
                                new SelfAssessmentOption("mood_balance_3", "감정 기복이 매우 심했고 지치거나 무기력했다", 4)
                        )
                ),
                new SelfAssessmentQuestion(
                        "sleep_quality",
                        "psychology",
                        "수면의 질은 어떤가요?",
                        List.of(
                                new SelfAssessmentOption("sleep_quality_0", "충분히 숙면을 취하고 있다", 0),
                                new SelfAssessmentOption("sleep_quality_1", "가끔 뒤척이지만 전반적으로 괜찮다", 1),
                                new SelfAssessmentOption("sleep_quality_2", "자주 뒤척이거나 꿈을 많이 꿔 피곤하다", 3),
                                new SelfAssessmentOption("sleep_quality_3", "잠들기 어렵거나 자주 깨는 편이다", 4)
                        )
                ),
                new SelfAssessmentQuestion(
                        "energy_level",
                        "psychology",
                        "하루 에너지 수준은 어떤가요?",
                        List.of(
                                new SelfAssessmentOption("energy_level_0", "활력이 넘치고 집중이 잘 된다", 0),
                                new SelfAssessmentOption("energy_level_1", "일상 활동에 필요한 에너지는 유지된다", 1),
                                new SelfAssessmentOption("energy_level_2", "쉽게 피로하고 집중이 어렵다", 3),
                                new SelfAssessmentOption("energy_level_3", "기본적인 활동도 버겁게 느껴진다", 4)
                        )
                ),
                new SelfAssessmentQuestion(
                        "support_system",
                        "psychology",
                        "주변의 정서적 지지(가족/친구)를 얼마나 느끼나요?",
                        List.of(
                                new SelfAssessmentOption("support_system_0", "항상 충분한 지지를 받고 있다", 0),
                                new SelfAssessmentOption("support_system_1", "대체로 지지를 받지만 더 필요할 때가 있다", 1),
                                new SelfAssessmentOption("support_system_2", "필요한 도움을 받기 어려울 때가 많다", 3),
                                new SelfAssessmentOption("support_system_3", "거의 혼자라고 느끼거나 지지 체계가 없다", 4)
                        )
                ),
                new SelfAssessmentQuestion(
                        "self_care",
                        "psychology",
                        "최근 스스로를 돌보는 시간(운동, 취미 등)을 얼마나 확보했나요?",
                        List.of(
                                new SelfAssessmentOption("self_care_0", "규칙적으로 시간을 내어 돌보고 있다", 0),
                                new SelfAssessmentOption("self_care_1", "가끔 시간을 내지만 꾸준하진 않다", 1),
                                new SelfAssessmentOption("self_care_2", "거의 시간을 내지 못했다", 3),
                                new SelfAssessmentOption("self_care_3", "전혀 여유가 없어 돌보지 못했다", 4)
                        )
                )
        );

        List<SelfAssessmentGuide> psychologyGuides = List.of(
                new SelfAssessmentGuide(
                        "안정",
                        0,
                        4,
                        "정서가 비교적 안정적으로 유지되고 있습니다.",
                        "현재 유지 중인 자기 돌봄 루틴을 이어가며, 긍정적인 활동을 꾸준히 실천해 보세요."
                ),
                new SelfAssessmentGuide(
                        "주의",
                        5,
                        11,
                        "스트레스 신호가 서서히 누적되고 있습니다.",
                        "하루 중 짧은 휴식 시간을 늘리고, 신뢰하는 사람과 마음을 나누며 긴장을 조절해 보세요."
                ),
                new SelfAssessmentGuide(
                        "집중 지원",
                        12,
                        20,
                        "높은 수준의 정서적 부담이 감지됩니다.",
                        "전문 상담이나 지역의 지원 자원을 활용해 도움을 요청하고, 회복 시간을 최우선으로 확보해 보세요."
                )
        );

        catalog.put("psychology", SelfAssessmentForm.create(
                "psychology",
                "감정 · 스트레스 점검",
                "최근 한 주간 감정과 생활 리듬을 살펴보는 자가 설문입니다.",
                psychologyQuestions,
                psychologyGuides
        ));
        List<SelfAssessmentQuestion> careerQuestions = List.of(
                new SelfAssessmentQuestion(
                        "career_clarity",
                        "career",
                        "현재 추구하고 싶은 진로 방향이 얼마나 명확한가요?",
                        List.of(
                                new SelfAssessmentOption("career_clarity_0", "목표가 뚜렷하고 실행 계획이 있다", 0),
                                new SelfAssessmentOption("career_clarity_1", "대략적인 방향은 있지만 세부 계획은 부족하다", 1),
                                new SelfAssessmentOption("career_clarity_2", "여러 선택지 사이에서 갈등이 크다", 3),
                                new SelfAssessmentOption("career_clarity_3", "무엇을 원하는지조차 모르겠다", 4)
                        )
                ),
                new SelfAssessmentQuestion(
                        "skill_confidence",
                        "career",
                        "목표 진로에 필요한 역량에 대한 자신감은 어떤가요?",
                        List.of(
                                new SelfAssessmentOption("skill_confidence_0", "현재 역량으로 충분하다고 느낀다", 0),
                                new SelfAssessmentOption("skill_confidence_1", "어느 정도 준비되어 있으나 보완이 필요하다", 1),
                                new SelfAssessmentOption("skill_confidence_2", "준비가 부족하다고 느껴 불안하다", 3),
                                new SelfAssessmentOption("skill_confidence_3", "어디서부터 시작해야 할지 모르겠다", 4)
                        )
                ),
                new SelfAssessmentQuestion(
                        "work_satisfaction",
                        "career",
                        "현재 하고 있는 일(학업 포함)에 대한 만족도는 어떤가요?",
                        List.of(
                                new SelfAssessmentOption("work_satisfaction_0", "대체로 만족하며 의미를 느낀다", 0),
                                new SelfAssessmentOption("work_satisfaction_1", "만족과 불만이 공존하지만 견딜 만하다", 1),
                                new SelfAssessmentOption("work_satisfaction_2", "불만이 커서 동기 유지가 어렵다", 3),
                                new SelfAssessmentOption("work_satisfaction_3", "즉시 변화를 추구하고 싶을 만큼 힘들다", 4)
                        )
                ),
                new SelfAssessmentQuestion(
                        "network_support",
                        "career",
                        "진로 고민을 함께 나눌 수 있는 사람이나 네트워크가 있나요?",
                        List.of(
                                new SelfAssessmentOption("network_support_0", "충분한 네트워크와 조언을 받고 있다", 0),
                                new SelfAssessmentOption("network_support_1", "몇몇 조언자는 있지만 더 확장하고 싶다", 1),
                                new SelfAssessmentOption("network_support_2", "상담할 사람이 거의 없어 답답하다", 3),
                                new SelfAssessmentOption("network_support_3", "완전히 혼자 해결해야 한다고 느낀다", 4)
                        )
                ),
                new SelfAssessmentQuestion(
                        "job_stress",
                        "career",
                        "진로와 관련된 스트레스 수준은 어느 정도인가요?",
                        List.of(
                                new SelfAssessmentOption("job_stress_0", "감당 가능한 수준이며 잘 관리되고 있다", 0),
                                new SelfAssessmentOption("job_stress_1", "스트레스가 있지만 휴식으로 조절 가능하다", 1),
                                new SelfAssessmentOption("job_stress_2", "스트레스가 높아 생활 전반에 영향을 준다", 3),
                                new SelfAssessmentOption("job_stress_3", "매우 높은 스트레스로 긴급한 도움이 필요하다", 4)
                        )
                )
        );

        List<SelfAssessmentGuide> careerGuides = List.of(
                new SelfAssessmentGuide(
                        "안정",
                        0,
                        4,
                        "진로 방향에 대한 자신감이 비교적 안정적입니다.",
                        "현재 진행 중인 탐색과 학습을 유지하면서 강점을 꾸준히 강화해 보세요."
                ),
                new SelfAssessmentGuide(
                        "탐색 강화",
                        5,
                        11,
                        "진로에 대한 불확실성과 부담이 함께 느껴집니다.",
                        "관심 분야를 더 탐색하고, 필요한 역량을 구체화해 실천 계획을 세워보세요."
                ),
                new SelfAssessmentGuide(
                        "집중 지원",
                        12,
                        20,
                        "진로 스트레스가 높아 즉각적인 지원이 필요해 보입니다.",
                        "커리어 코칭, 취업 상담 등 전문적인 도움을 활용해 현실적인 전략을 마련해 보세요."
                )
        );

        catalog.put("career", SelfAssessmentForm.create(
                "career",
                "진로 탐색 부담도",
                "현재 진로 방향과 준비도를 점검해 보는 자가 설문입니다.",
                careerQuestions,
                careerGuides
        ));

        return Collections.unmodifiableMap(catalog);
    }
}
