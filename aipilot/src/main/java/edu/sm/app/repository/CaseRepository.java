package edu.sm.app.repository;

import edu.sm.app.dto.Case;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 사건 Repository (aipilot에서 사용)
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
    Case select(@Param("caseId") int caseId) throws Exception;

    /**
     * 사건 수정
     */
    void update(Case trialCase) throws Exception;

    /**
     * 상태별 조회
     */
    List<Case> selectByStatus(@Param("status") String status) throws Exception;
}