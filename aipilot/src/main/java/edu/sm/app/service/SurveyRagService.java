package edu.sm.app.service;

import edu.sm.app.dto.CounselingRequest;
import edu.sm.app.dto.CounselingResponse;
import edu.sm.app.dto.SurveyRecord;
import edu.sm.app.dto.SurveySubmission;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class SurveyRagService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final Map<String, List<SurveyRecord>> surveyStore = new ConcurrentHashMap<>();

    public String storeSurvey(SurveySubmission submission) {
        if (submission == null) {
            throw new IllegalArgumentException("설문 정보가 비어 있습니다.");
        }

        String clientId = normalize(submission.clientId());
        String category = normalize(submission.category());

        if (clientId == null || category == null) {
            throw new IllegalArgumentException("clientId와 category는 필수 항목입니다.");
        }

        String sessionFocus = normalize(submission.sessionFocus());
        String keyObservations = normalize(submission.keyObservations());
        String supportNeeds = normalize(submission.supportNeeds());
        String nextSteps = normalize(submission.nextSteps());

        String surveyId = UUID.randomUUID().toString();
        String submittedAt = ZonedDateTime.now(ZoneOffset.UTC).format(DATE_TIME_FORMATTER);

        SurveyRecord surveyRecord = new SurveyRecord(
                surveyId,
                clientId,
                category,
                sessionFocus,
                keyObservations,
                supportNeeds,
                nextSteps,
                submittedAt
        );

        surveyStore
                .computeIfAbsent(clientId, key -> new CopyOnWriteArrayList<>())
                .add(surveyRecord);

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
                        .map(cat -> cat.equalsIgnoreCase(normalize(record.category())))
                        .orElse(true))
                .sorted(Comparator.comparing(this::parseSubmittedAt).reversed())
                .collect(Collectors.toCollection(ArrayList::new));

        return Collections.unmodifiableList(filtered);
    }

    public CounselingResponse generateCounseling(CounselingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("상담 요청이 비어 있습니다.");
        }

        String clientId = normalize(request.clientId());
        String question = normalize(request.question());

        if (clientId == null || question == null) {
            throw new IllegalArgumentException("clientId와 question은 필수 항목입니다.");
        }

        Optional<String> category = Optional.ofNullable(normalize(request.category()));
        List<SurveyRecord> references = findSurveys(clientId, category);

        String advice;
        if (references.isEmpty()) {
            advice = "최근에 제출된 설문이 없어 일반적인 조언을 제공합니다. 질문하신 내용은 '" +
                    question + "'이며, 꾸준히 상황을 기록하면 더 맞춤형 지원을 받을 수 있습니다.";
        } else {
            SurveyRecord latest = references.get(0);
            StringBuilder builder = new StringBuilder();
            builder.append("질문하신 내용 '")
                    .append(question)
                    .append("'에 대해 최근 상담 목표 '")
                    .append(valueOrPlaceholder(latest.sessionFocus()))
                    .append("'를 바탕으로 다음과 같이 제안드립니다. ");

            if (latest.keyObservations() != null) {
                builder.append("주요 관찰 내용은 '")
                        .append(latest.keyObservations())
                        .append("'이며, ");
            }

            if (latest.supportNeeds() != null) {
                builder.append("필요한 지원으로는 '")
                        .append(latest.supportNeeds())
                        .append("'가 언급되었습니다. ");
            }

            if (latest.nextSteps() != null) {
                builder.append("다음 단계로 '")
                        .append(latest.nextSteps())
                        .append("'을(를) 고려해 보세요. ");
            }

            builder.append("추가 질문이 있다면 계속해서 알려주세요.");
            advice = builder.toString();
        }

        return new CounselingResponse(advice, references.isEmpty() ? null : references);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ZonedDateTime parseSubmittedAt(SurveyRecord record) {
        if (record.submittedAt() == null) {
            return ZonedDateTime.now(ZoneOffset.UTC);
        }
        return ZonedDateTime.parse(record.submittedAt(), DATE_TIME_FORMATTER);
    }

    private String valueOrPlaceholder(String value) {
        return value != null ? value : "미기록";
    }
}