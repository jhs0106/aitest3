package edu.sm.app.repository;

import edu.sm.app.dto.Case;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 사건 Repository
 */
@Repository
@Mapper
public interface CaseRepository {

    /**
     * 전체 사건 조회
     */
    List<Case> selectAll() throws Exception;

    /**
     * ID로 사건 조회
     */
    Case select(int caseId) throws Exception;

    /**
     * 사건 등록
     */
    void insert(Case trialCase) throws Exception;

    /**
     * 사건 수정
     */
    void update(Case trialCase) throws Exception;

    /**
     * 사건 삭제
     */
    void delete(int caseId) throws Exception;

    /**
     * 상태별 조회
     */
    List<Case> selectByStatus(String status) throws Exception;

    /**
     * 피고인별 조회
     */
    List<Case> selectByDefendant(String defendant) throws Exception;

    /**
     * 사건번호로 조회
     */
    Case selectByCaseNumber(String caseNumber) throws Exception;
}