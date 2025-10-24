package edu.sm.app.service;

import edu.sm.app.dto.DoorAccessRecord;
import edu.sm.app.dto.DoorUser;
import edu.sm.app.mapper.DoorUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DoorUserService {

    private final DoorUserMapper doorUserMapper;

    @Transactional
    public void registerUser(DoorUser user) {
        doorUserMapper.insertUser(user);
        log.info("👤 DB 사용자 등록 완료: {}", user.getName());
    }

    public DoorUser findUserByName(String name) {
        return doorUserMapper.findUserByName(name);
    }

    public List<DoorUser> findAllUsers() {
        return doorUserMapper.findAllUsers();
    }

    @Transactional
    public void logAccess(String name, String status) {
        DoorAccessRecord record = DoorAccessRecord.builder()
                .name(name)
                .status(status)
                .build();
        doorUserMapper.insertAccessRecord(record);
        log.info("📢 DB 출입 기록 저장: {} ({})", name, status);
    }

    public List<DoorAccessRecord> findAllRecords() {
        return doorUserMapper.findAllRecords();
    }

    // AI Context에 제공할 등록된 사용자 얼굴 특징 텍스트 생성
    public String getAllFaceSignatures() {
        List<DoorUser> users = findAllUsers();
        if (users.isEmpty()) {
            return "등록된 사용자가 없습니다.";
        }
        return users.stream()
                .map(user -> String.format("Name: %s, Signature: %s", user.getName(), user.getFaceSignature()))
                .collect(java.util.stream.Collectors.joining("\n"));
    }
}