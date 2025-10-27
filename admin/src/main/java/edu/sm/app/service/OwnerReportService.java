package edu.sm.app.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * /ai6/admin/owner-ask -> 여기 -> OpsGuideRagService.askOwnerWithOpsGuide()
 *
 * 사장님 화면(ownerreport.jsp)에서 질문하면 결국 여기로 들어오고,
 * 여기서 KPI+RAG 결과를 OwnerAnswer(answer: "...") 형태로 넘겨서
 * 프론트 JS가 data.answer로 읽는다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerReportService {

    private final OpsGuideRagService opsGuideRagService;

    public OwnerAnswer askOwnerAI(String question) {

        // KPI + RAG 통합 답변
        OpsGuideRagService.OwnerOpsAnswer raw = opsGuideRagService.askOwnerWithOpsGuide(question);

        OwnerAnswer dto = new OwnerAnswer();
        dto.setAnswer(raw.getAnswer());
        return dto;
    }

    @Data
    public static class OwnerAnswer {
        private String answer; // 프론트는 이 필드를 그대로 사용함
    }
}
