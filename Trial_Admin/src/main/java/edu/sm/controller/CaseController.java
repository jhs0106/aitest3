package edu.sm.controller;

import edu.sm.app.dto.Case;
import edu.sm.app.service.CaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Trial_Admin - 사건 관리 컨트롤러 (실제 DB 연동)
 */
@Controller
@Slf4j
@RequestMapping("/case")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;
    String dir = "case/";

    /**
     * 사건 등록 화면
     */
    @RequestMapping("/register")
    public String register(Model model) {
        model.addAttribute("center", dir + "register");
        model.addAttribute("left", "left");
        return "index";
    }

    /**
     * 사건 목록 화면
     */
    @RequestMapping("/list")
    public String list(Model model) {
        try {
            List<Case> caseList = caseService.getAllCases();
            model.addAttribute("caseList", caseList);
        } catch (Exception e) {
            log.error("사건 목록 조회 실패", e);
            model.addAttribute("errorMessage", "사건 목록을 불러오는 중 오류가 발생했습니다.");
        }

        model.addAttribute("center", dir + "list");
        model.addAttribute("left", "left");
        return "index";
    }

    /**
     * 사건 상세 화면
     */
    @RequestMapping("/detail")
    public String detail(@RequestParam("id") Integer caseId, Model model) {
        try {
            Case trialCase = caseService.getCaseById(caseId);
            model.addAttribute("trialCase", trialCase);
        } catch (Exception e) {
            log.error("사건 조회 실패 - ID: {}", caseId, e);
            model.addAttribute("errorMessage", "사건 정보를 불러오는 중 오류가 발생했습니다.");
        }

        model.addAttribute("center", dir + "detail");
        model.addAttribute("left", "left");
        return "index";
    }

    /**
     * 사건 통계 화면
     */
    @RequestMapping("/statistics")
    public String statistics(Model model) {
        try {
            List<Case> allCases = caseService.getAllCases();

            // 통계 계산
            long totalCases = allCases.size();
            long criminalCases = allCases.stream().filter(c -> "criminal".equals(c.getCaseType())).count();
            long civilCases = allCases.stream().filter(c -> "civil".equals(c.getCaseType())).count();
            long inProgressCases = allCases.stream().filter(c -> "in_progress".equals(c.getStatus())).count();

            model.addAttribute("totalCases", totalCases);
            model.addAttribute("criminalCases", criminalCases);
            model.addAttribute("civilCases", civilCases);
            model.addAttribute("inProgressCases", inProgressCases);

        } catch (Exception e) {
            log.error("통계 조회 실패", e);
        }

        model.addAttribute("center", dir + "statistics");
        model.addAttribute("left", "left");
        return "index";
    }

    /**
     * 사건 등록 처리
     */
    @RequestMapping("/register-impl")
    public String registerImpl(
            @RequestParam("caseType") String caseType,
            @RequestParam("defendant") String defendant,
            @RequestParam("charge") String charge,
            @RequestParam(value = "description", required = false) String description,
            Model model) {

        try {
            Case trialCase = Case.builder()
                    .caseType(caseType)
                    .defendant(defendant)
                    .charge(charge)
                    .description(description)
                    .build();

            Integer caseId = caseService.registerCase(trialCase);
            log.info("사건 등록 성공 - ID: {}, 사건번호: {}", caseId, trialCase.getCaseNumber());

            model.addAttribute("message", "사건이 성공적으로 등록되었습니다. (사건번호: " + trialCase.getCaseNumber() + ")");

        } catch (Exception e) {
            log.error("사건 등록 실패", e);
            model.addAttribute("errorMessage", "사건 등록 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/case/list";
    }

    /**
     * 사건 삭제
     */
    @RequestMapping("/delete")
    public String delete(@RequestParam("id") Integer caseId, Model model) {
        try {
            caseService.deleteCase(caseId);
            log.info("사건 삭제 완료 - ID: {}", caseId);
        } catch (Exception e) {
            log.error("사건 삭제 실패 - ID: {}", caseId, e);
            model.addAttribute("errorMessage", "사건 삭제 중 오류가 발생했습니다.");
        }

        return "redirect:/case/list";
    }
}