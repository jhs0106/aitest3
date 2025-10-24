package edu.sm.app.service;

import edu.sm.app.dto.CounselingRequest;
import edu.sm.app.dto.CounselingResponse;
import edu.sm.app.dto.SurveyRecord;
import edu.sm.app.dto.SurveySubmission;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SurveyRagService {

    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public SurveyRagService(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
    }

    public String storeSurvey(SurveySubmission submission) {
        Instant now = Instant.now();
        String surveyId = UUID.randomUUID().toString();

        String clientId = required(submission.clientId(), "clientId");
        String category = required(submission.category(), "category");

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("surveyId", surveyId);
        metadata.put("clientId", clientId);
        metadata.put("category", category);
        metadata.put("sessionFocus", sanitize(submission.sessionFocus()));
        metadata.put("keyObservations", sanitize(submission.keyObservations()));
        metadata.put("supportNeeds", sanitize(submission.supportNeeds()));
        metadata.put("nextSteps", sanitize(submission.nextSteps()));
        metadata.put("submittedAt", now.toString());

        String content = buildDocumentContent(surveyId, clientId, category, submission, now);

        Document document = new Document(surveyId, content, metadata);
        vectorStore.add(List.of(document));
        return surveyId;
    }

    public List<SurveyRecord> findSurveys(String clientId, Optional<String> category) {
        if (!StringUtils.hasText(clientId)) {
            return List.of();
        }
        SearchRequest request = buildSearchRequest(buildQuery(clientId, category, null), 20);
        return vectorStore.similaritySearch(request).stream()
                .filter(document -> matches(document, clientId, category))
                .sorted(Comparator.comparing(this::readSubmittedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::toRecord)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public CounselingResponse generateCounseling(CounselingRequest request) {
        String clientId = request.clientId();
        String question = request.question();
        Optional<String> category = Optional.ofNullable(
                StringUtils.hasText(request.category()) ? request.category().trim() : null);

        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(question)) {
            return new CounselingResponse("상담을 위해서는 내담자 ID와 질문이 필요합니다.", List.of());
        }

        SearchRequest searchRequest = buildSearchRequest(buildQuery(clientId, category, question), 10);
        List<Document> documents = vectorStore.similaritySearch(searchRequest).stream()
                .filter(document -> matches(document, clientId, category))
                .collect(Collectors.toList());

        if (documents.isEmpty()) {
            return new CounselingResponse("저장된 설문 기록이 없습니다. 먼저 설문을 입력해 주세요.", List.of());
        }

        String context = documents.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n\n"));

        String prompt = "다음은 내담자의 설문 기록입니다. 이 정보를 기반으로 공감적인 상담 답변을 제공해 주세요.\n\n" +
                context +
                "\n\n내담자 질문: " + question + "\n" +
                "답변은 한국어로 작성하고, 설문에 기록된 사실을 인용해 실질적인 조언을 제시하세요.";

        ChatResponse response = chatClient.prompt()
                .system(spec -> spec.text("너는 섬세하고 공감 능력이 뛰어난 상담사다. 설문 기록을 근거로 진로/심리 상담을 제공하라."))
                .user(spec -> spec.text(prompt))
                .call();

        String advice = extractAdvice(response);
        List<SurveyRecord> references = documents.stream()
                .map(this::toRecord)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new CounselingResponse(advice, references);
    }

    private String extractAdvice(ChatResponse response) {
        if (response == null) {
            return "응답을 생성하지 못했습니다.";
        }
        String content = readFirstContent(response);
        if (StringUtils.hasText(content)) {
            return content;
        }
        return "응답을 생성하지 못했습니다.";
    }

    private String readFirstContent(ChatResponse response) {
        if (response == null) {
            return "";
        }
        if (response.getResult() != null && response.getResult().getOutput() != null
                && StringUtils.hasText(response.getResult().getOutput().getContent())) {
            return response.getResult().getOutput().getContent();
        }
        if (response.getResults() == null) {
            return "";
        }
        return response.getResults().stream()
                .filter(Objects::nonNull)
                .map(result -> result.getOutput())
                .filter(Objects::nonNull)
                .map(output -> output.getContent())
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("");
    }

    private SurveyRecord toRecord(Document document) {
        if (document == null) {
            return null;
        }
        Map<String, Object> metadata = document.getMetadata();
        if (metadata == null) {
            metadata = new LinkedHashMap<>();
        }
        String submittedAtIso = value(metadata.get("submittedAt"));
        String submittedAt = "";
        Instant instant = null;
        if (StringUtils.hasText(submittedAtIso)) {
            try {
                instant = Instant.parse(submittedAtIso);
                submittedAt = DISPLAY_FORMATTER.format(instant);
            } catch (Exception ignored) {
                submittedAt = submittedAtIso;
            }
        }

        return new SurveyRecord(
                firstNonBlank(value(metadata.get("surveyId")), document.getId()),
                value(metadata.get("clientId")),
                value(metadata.get("category")),
                value(metadata.get("sessionFocus")),
                value(metadata.get("keyObservations")),
                value(metadata.get("supportNeeds")),
                value(metadata.get("nextSteps")),
                submittedAt
        );
    }

    private String buildDocumentContent(String surveyId, String clientId, String category, SurveySubmission submission, Instant submittedAt) {
        String sessionFocus = placeholder(submission.sessionFocus());
        String keyObservations = placeholder(submission.keyObservations());
        String supportNeeds = placeholder(submission.supportNeeds());
        String nextSteps = placeholder(submission.nextSteps());

        return """
                ClientTag:CLIENT_%s
                CategoryTag:CATEGORY_%s
                SurveyId:%s
                SubmittedAt:%s
                SessionFocus:%s
                KeyObservations:%s
                SupportNeeds:%s
                NextSteps:%s
                """.formatted(
                clientId,
                category,
                surveyId,
                submittedAt,
                sessionFocus,
                keyObservations,
                supportNeeds,
                nextSteps
        );
    }

    private String buildQuery(String clientId, Optional<String> category, String question) {
        StringBuilder builder = new StringBuilder();
        builder.append("CLIENT_").append(clientId.trim());
        category.ifPresent(cat -> builder.append(' ').append("CATEGORY_").append(cat.trim()));
        if (StringUtils.hasText(question)) {
            builder.append(' ').append(question.trim());
        }
        return builder.toString();
    }

    private SearchRequest buildSearchRequest(String query, int topK) {
        return SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();
    }

    private String sanitize(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String required(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String placeholder(String value) {
        return StringUtils.hasText(value) ? value.trim() : "(응답 없음)";
    }

    private String value(Object object) {
        return object == null ? "" : object.toString();
    }

    private String firstNonBlank(String first, String fallback) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        return StringUtils.hasText(fallback) ? fallback : "";
    }

    private Instant readSubmittedAt(Document document) {
        if (document == null || document.getMetadata() == null) {
            return null;
        }
        Object submittedAt = document.getMetadata().get("submittedAt");
        if (submittedAt instanceof Instant instant) {
            return instant;
        }
        if (submittedAt instanceof String str && StringUtils.hasText(str)) {
            try {
                return Instant.parse(str);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean matches(Document document, String clientId, Optional<String> category) {
        if (document == null || document.getMetadata() == null) {
            return false;
        }

        Map<String, Object> metadata = document.getMetadata();
        String docClientId = value(metadata.get("clientId")).trim();
        if (!StringUtils.hasText(docClientId)) {
            return false;
        }

        if (!docClientId.equalsIgnoreCase(clientId.trim())) {
            return false;
        }

        if (category.isPresent()) {
            String docCategory = value(metadata.get("category")).trim();
            if (!StringUtils.hasText(docCategory) || !docCategory.equalsIgnoreCase(category.get().trim())) {
                return false;
            }
        }

        return true;
    }
}
