package edu.sm.controller;

import edu.sm.app.dto.DoorAccessRecord;
import edu.sm.app.dto.DoorUser;
import edu.sm.app.service.DoorUserService;
import edu.sm.app.service.FaceRecognitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;

// DOORS 기능의 REST API를 처리하는 컨트롤러
@RestController
@RequestMapping("/doors/api")
@Slf4j
@RequiredArgsConstructor
public class DoorApiController {

    final private FaceRecognitionService faceRecognitionService;
    final private DoorUserService doorUserService; // 변경: MockImpl -> DoorUserService

    // 1. 사용자 등록 API
    @RequestMapping(value = "/register")
    public ResponseEntity<String> registerUser(
            @RequestParam("name") String name,
            @RequestParam("attach") MultipartFile attach) {
        try {
            if (attach.isEmpty() || !attach.getContentType().contains("image/")) {
                return ResponseEntity.badRequest().body("이미지 파일을 업로드해 주세요.");
            }

            // 1. AI로 얼굴 특징 추출
            String faceSignature = faceRecognitionService.extractFaceSignature(name, attach.getContentType(), attach.getBytes());

            // 2. 추출된 특징을 DB에 등록 (MyBatis 호출)
            DoorUser user = DoorUser.builder()
                    .name(name)
                    .faceSignature(faceSignature)
                    .build();
            doorUserService.registerUser(user);

            return ResponseEntity.ok(String.format("등록 완료: %s. AI 특징: %s", name, faceSignature));

        } catch (Exception e) {
            log.error("사용자 등록 실패", e);
            // 데이터베이스 연동 에러 포함 가능성 있음
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("등록 중 서버 오류가 발생했습니다. 로그를 확인하세요.");
        }
    }

    // 2. 얼굴 인식 및 문 제어 API (동일)
    @RequestMapping(value = "/recognition")
    public Flux<String> recognize(@RequestParam("attach") MultipartFile attach) throws IOException {
        if (attach == null || !attach.getContentType().contains("image/")) {
            return Flux.just("이미지를 올려주세요.");
        }
        return faceRecognitionService.recognizeAndControl(attach);
    }

    // 3. 출입 기록 조회 API (동일)
    @RequestMapping(value = "/records")
    public List<DoorAccessRecord> getRecords() {
        return doorUserService.findAllRecords();
    }
}