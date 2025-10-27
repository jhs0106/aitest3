package edu.sm.app.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class TrialRoleTools {

    /**
     * 판사 - 개정 선언
     */
    @Tool(description = "판사가 법정을 개정하고 재판을 시작합니다")
    public OpenTrialResponse judgeOpenTrial(OpenTrialRequest request) {  // ⭐ Function 제거
        log.info("개정 선언 - 사건번호: {}, 피고인: {}", request.caseNumber(), request.defendant());

        String declaration = String.format("""
            ⚖️ 개정을 선언합니다.
            
            사건번호: %s
            피고인: %s님
            혐의: %s
            
            공정하고 엄정한 재판을 진행하겠습니다.
            """, request.caseNumber(), request.defendant(), request.charge());

        return new OpenTrialResponse(
                "SUCCESS",
                declaration,
                "재판이 개정되었습니다."
        );
    }

    /**
     * 검사 - 기소 및 구형
     */
    @Tool(description = "검사가 피고인을 기소하고 형량을 구형합니다")
    public ProsecuteResponse prosecutorProsecute(ProsecuteRequest request) {  // ⭐ Function 제거
        log.info("검사 구형 - 피고인: {}, 요구형량: {}", request.defendant(), request.requestedSentence());

        String prosecution = String.format("""
            👔 검사 의견
            
            피고인 %s는 %s 혐의로 기소되었습니다.
            
            증거:
            %s
            
            피고인의 행위는 명백한 법률 위반이며,
            사회 질서 유지를 위해 %s를 구형합니다.
            """,
                request.defendant(),
                request.charge(),
                request.evidence(),
                request.requestedSentence()
        );

        return new ProsecuteResponse(
                "SUCCESS",
                prosecution,
                request.requestedSentence()
        );
    }

    /**
     * 변호사 - 변론 및 감형 요청
     */
    @Tool(description = "변호사가 피고인을 변론하고 정상참작을 요청합니다")
    public DefendResponse attorneyDefend(DefendRequest request) {  // ⭐ Function 제거
        log.info("변호사 변론 - 피고인: {}, 정상참작: {}", request.defendant(), request.mitigatingFactors());

        String defense = String.format("""
            👨‍⚖️ 변호인 의견
            
            피고인 %s를 변론하겠습니다.
            
            정상참작 사유:
            %s
            
            피고인은 깊이 반성하고 있으며,
            관대한 처분을 부탁드립니다.
            """,
                request.defendant(),
                request.mitigatingFactors()
        );

        return new DefendResponse(
                "SUCCESS",
                defense,
                request.mitigatingFactors()
        );
    }

    /**
     * 판사 - 최종 판결
     */
    @Tool(description = "판사가 최종 판결을 선고합니다")
    public VerdictResponse judgeVerdict(VerdictRequest request) {  // ⭐ Function 제거
        String defendant = StringUtils.hasText(request.defendant()) ? request.defendant() : "피고인";
        String verdict = StringUtils.hasText(request.verdict()) ? request.verdict() : "선고유예";
        String reason = StringUtils.hasText(request.reason()) ? request.reason() : "제출된 진술과 증거를 종합한 결과, 형의 선고가 적정하다고 판단하였습니다.";

        log.info("판결 선고 - 피고인: {}, 판결: {}", defendant, verdict);

        String sentenceText = String.format("""
            ⚖️ 판결 선고
            
            피고인 %s에 대한 판결을 선고합니다.
            
            주문:
            피고인을 %s에 처한다.
            
            이유:
            %s
            
            이상으로 판결을 마칩니다.
            """,
                defendant,
                verdict,
                reason
        );

        return new VerdictResponse(
                "SUCCESS",
                sentenceText,
                verdict,
                reason
        );
    }

    // ===== Request/Response 레코드 클래스 =====

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("개정 선언 요청")
    public record OpenTrialRequest(
            @JsonProperty(required = true, value = "caseNumber")
            @JsonPropertyDescription("사건번호 (예: 2025고단1234)")
            String caseNumber,

            @JsonProperty(required = true, value = "defendant")
            @JsonPropertyDescription("피고인 이름")
            String defendant,

            @JsonProperty(required = true, value = "charge")
            @JsonPropertyDescription("혐의 내용")
            String charge
    ) {}

    public record OpenTrialResponse(
            String status,
            String declaration,
            String message
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("검사 기소 요청")
    public record ProsecuteRequest(
            @JsonProperty(required = true, value = "defendant")
            @JsonPropertyDescription("피고인 이름")
            String defendant,

            @JsonProperty(required = true, value = "charge")
            @JsonPropertyDescription("혐의 내용")
            String charge,

            @JsonProperty(required = true, value = "evidence")
            @JsonPropertyDescription("증거 목록")
            String evidence,

            @JsonProperty(required = true, value = "requestedSentence")
            @JsonPropertyDescription("요구 형량 (예: 징역 6개월)")
            String requestedSentence
    ) {}

    public record ProsecuteResponse(
            String status,
            String prosecution,
            String requestedSentence
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("변호사 변론 요청")
    public record DefendRequest(
            @JsonProperty(required = true, value = "defendant")
            @JsonPropertyDescription("피고인 이름")
            String defendant,

            @JsonProperty(required = true, value = "mitigatingFactors")
            @JsonPropertyDescription("정상참작 사유")
            String mitigatingFactors
    ) {}

    public record DefendResponse(
            String status,
            String defense,
            String mitigatingFactors
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("판결 선고 요청")
    public record VerdictRequest(
            @JsonProperty(value = "defendant")
            @JsonPropertyDescription("피고인 이름")
            String defendant,

            @JsonProperty(value = "verdict")
            @JsonPropertyDescription("판결 내용 (예: 징역 3개월, 집행유예 1년)")
            String verdict,

            @JsonProperty(value = "reason")
            @JsonPropertyDescription("판결 이유")
            String reason
    ) {}

    public record VerdictResponse(
            String status,
            String sentenceText,
            String verdict,
            String reason
    ) {}
}