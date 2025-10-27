package edu.sm.app.service;

import edu.sm.app.dto.Case;
import edu.sm.app.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사건 관리 서비스 (aipilot용 - 조회 및 수정만)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CaseService {

    private final CaseRepository caseRepository;

    /**
     * 전체 사건 조회
     */
    public List<Case> getAllCases() throws Exception {
        return caseRepository.selectAll();
    }

    /**
     * ID로 사건 조회
     */
    public Case getCaseById(Integer caseId) throws Exception {
        return caseRepository.select(caseId);
    }

    /**
     * 사건 수정 (상태 변경 등)
     */
    public void updateCase(Case trialCase) throws Exception {
        log.info("사건 수정 - ID: {}, 상태: {}", trialCase.getCaseId(), trialCase.getStatus());
        trialCase.setUpdatedAt(LocalDateTime.now());
        caseRepository.update(trialCase);
    }

    /**
     * 상태별 조회
     */
    public List<Case> getCasesByStatus(String status) throws Exception {
        return caseRepository.selectByStatus(status);
    }
}