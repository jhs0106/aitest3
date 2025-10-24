package edu.sm.app.mapper;

import edu.sm.app.dto.DoorAccessRecord;
import edu.sm.app.dto.DoorUser;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DoorUserMapper {
    // 사용자 등록
    void insertUser(DoorUser user);

    // 사용자 이름으로 조회
    DoorUser findUserByName(String name);

    // 모든 사용자 조회 (AI Context용)
    List<DoorUser> findAllUsers();

    // 출입 기록 등록
    void insertAccessRecord(DoorAccessRecord record);

    // 모든 출입 기록 조회
    List<DoorAccessRecord> findAllRecords();
}