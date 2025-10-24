package edu.sm.app.tool;

import edu.sm.app.dto.SelfAssessmentForm;
import edu.sm.app.dto.SelfAssessmentGuide;
import edu.sm.app.dto.SelfAssessmentOption;
import edu.sm.app.dto.SelfAssessmentQuestion;
import edu.sm.app.dto.SurveyAnswerRecord;
import edu.sm.app.dto.SurveyRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class SurveyRagTools {

    private SurveyRagTools() {
    }

    public static Map<String, SelfAssessmentForm> createFormCatalog() {
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
                                new SelfAssessmentOption("sleep_quality_3", "잠에 들기 어렵고 자주 깨서 휴식이 되지 않는다", 4)
                        )
                ),
                new SelfAssessmentQuestion(
                        "energy_level",
                        "psychology",
                        "평소 에너지 수준은 어떤가요?",
                        List.of(
                                new SelfAssessmentOption("energy_level_0", "일상에 필요한 에너지가 충분하다", 0),
                                new SelfAssessmentOption("energy_level_1", "대부분 견딜 만하지만 가끔 지친다", 1),
                                new SelfAssessmentOption("energy_level_2", "쉽게 피로감을 느끼고 회복이 어렵다", 3),
                                new SelfAssessmentOption("energy_level_3", "거의 매일 무기력하고 집중이 어렵다", 4)
                        )
                ),
                new SelfAssessmentQuestion(
                        "social_connection",
                        "psychology",
                        "최근 주변 사람들과의 관계는 어떠했나요?",
                        List.of(
                                new SelfAssessmentOption("social_connection_0", "지지와 공감을 충분히 받고 있다", 0),
                                new SelfAssessmentOption("social_connection_1", "필요할 때 도움을 요청할 사람이 있다", 1),
                                new SelfAssessmentOption("social_connection_2", "가까운 관계에서 거리를 느낀다", 3),
                                new SelfAssessmentOption("social_connection_3", "정서적으로 완전히 고립된 느낌이다", 4)
                        )
                ),
                new SelfAssessmentQuestion(
                        "emotion_regulation",
                        "psychology",
                        "감정을 조절하는 데 어려움이 있었나요?",
                        List.of(
                                new SelfAssessmentOption("emotion_regulation_0", "대부분 잘 조절했다", 0),
                                new SelfAssessmentOption("emotion_regulation_1", "가끔 감정이 격해졌지만 곧 안정되었다", 1),
                                new SelfAssessmentOption("emotion_regulation_2", "감정을 다루기 어려워 주변에 영향을 주었다", 3),
                                new SelfAssessmentOption("emotion_regulation_3", "감정 폭발 혹은 극도의 무기력이 반복되었다", 4)
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

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String buildFilterExpression(String clientId, Optional<String> categoryOptional) {
        StringBuilder filter = new StringBuilder("type == 'survey_record' && clientId == '")
                .append(escapeQuotes(clientId))
                .append("'");
        categoryOptional.ifPresent(cat -> filter.append(" && category == '").append(escapeQuotes(cat)).append("'"));
        return filter.toString();
    }

    public static String buildUserPrompt(String question,
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

    public static String buildFallbackAdvice(String question, List<SurveyRecord> references) {
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

    public static Optional<SelfAssessmentGuide> resolveGuide(SelfAssessmentForm form, int totalScore) {
        if (form.getGuides() == null) {
            return Optional.empty();
        }
        return form.getGuides().stream()
                .filter(guide -> totalScore >= guide.getMinScore() && totalScore <= guide.getMaxScore())
                .findFirst()
                .or(() -> form.getGuides().isEmpty() ? Optional.empty() : Optional.of(form.getGuides().get(form.getGuides().size() - 1)));
    }

    public static String summarizeResult(String category, int totalScore, int maxScore) {
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

    public static String recommendNextStep(String category, int totalScore, int maxScore) {
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
                    yield "커리어 코칭이나 취업 상담 등 외부 지원을 활용해 구체적인 행 전략을 세워보세요.";
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

    private static String formatSurveyRecord(SurveyRecord record) {
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
                    .collect(Collectors.toCollection(ArrayList::new));
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

    private static String escapeQuotes(String value) {
        return Objects.requireNonNullElse(value, "").replace("'", "''");
    }
}