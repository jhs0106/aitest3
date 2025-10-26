package edu.sm.app.service;

import edu.sm.app.dto.Case;
import edu.sm.app.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사건 관리 서비스
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CaseService {

    private final CaseRepository caseRepository;

    /**
     * 사건 등록
     *
     * @param trialCase 사건 정보
     * @return 등록된 사건 ID
     */
    public Integer registerCase(Case trialCase) throws Exception {
        log.info("사건 등록 - 피고인: {}, 혐의: {}", trialCase.getDefendant(), trialCase.getCharge());

        // 사건번호 자동 생성 (예: 2025고단0001)
        String caseNumber = generateCaseNumber(trialCase.getCaseType());
        trialCase.setCaseNumber(caseNumber);
        trialCase.setStatus("registered");
        trialCase.setCreatedAt(LocalDateTime.now());
        trialCase.setUpdatedAt(LocalDateTime.now());

        caseRepository.insert(trialCase);

        log.info("사건 등록 완료 - 사건번호: {}, ID: {}", caseNumber, trialCase.getCaseId());
        return trialCase.getCaseId();
    }

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
     * 사건 수정
     */
    public void updateCase(Case trialCase) throws Exception {
        log.info("사건 수정 - ID: {}", trialCase.getCaseId());
        trialCase.setUpdatedAt(LocalDateTime.now());
        caseRepository.update(trialCase);
    }

    /**
     * 사건 삭제
     */
    public void deleteCase(Integer caseId) throws Exception {
        log.info("사건 삭제 - ID: {}", caseId);
        caseRepository.delete(caseId);
    }

    /**
     * 상태별 조회
     */
    public List<Case> getCasesByStatus(String status) throws Exception {
        return caseRepository.selectByStatus(status);
    }

    /**
     * 피고인별 조회
     */
    public List<Case> getCasesByDefendant(String defendant) throws Exception {
        return caseRepository.selectByDefendant(defendant);
    }

    /**
     * 사건번호로 조회
     */
    public Case getCaseByCaseNumber(String caseNumber) throws Exception {
        return caseRepository.selectByCaseNumber(caseNumber);
    }

    /**
     * 사건번호 자동 생성
     *
     * @param caseType 사건 유형
     * @return 생성된 사건번호
     */
    private String generateCaseNumber(String caseType) throws Exception {
        // 현재 연도
        int year = LocalDateTime.now().getYear();

        // 해당 연도의 기존 사건 수 조회
        List<Case> allCases = caseRepository.selectAll();
        long countThisYear = allCases.stream()
                .filter(c -> c.getCaseNumber() != null && c.getCaseNumber().startsWith(String.valueOf(year)))
                .count();

        // 사건번호 생성 (예: 2025고단0001)
        String prefix = "criminal".equals(caseType) ? "고단" : "가단";
        return String.format("%d%s%04d", year, prefix, countThisYear + 1);
    }
}