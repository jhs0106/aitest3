package edu.sm.controller;

import edu.sm.app.service.CctvAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/cctv/api")
@Slf4j
@RequiredArgsConstructor
public class CctvApiController {

    final private CctvAnalysisService cctvAnalysisService;

    @RequestMapping(value = "/analysis")
    public Flux<String> analysis(
            @RequestParam("question") String question,
            @RequestParam("attach") MultipartFile attach) throws IOException {

        // 이미지가 업로드 되지 않았거나 이미지 파일이 아닐 경우
        if (attach == null || !attach.getContentType().contains("image/")) {
            return Flux.just("이미지를 올려주세요.");
        }

        Flux<String> flux = cctvAnalysisService.analyzeAndRespond(
                question, attach.getContentType(), attach.getBytes());
        return flux;
    }
}