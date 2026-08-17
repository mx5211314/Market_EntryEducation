package com.investedu.smartassistant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.investedu.smartassistant.entity.RiskAssessment;
import com.investedu.smartassistant.entity.User;
import com.investedu.smartassistant.service.RiskAssessmentService;
import com.investedu.smartassistant.service.UserService;
import com.investedu.smartassistant.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/assessment")
public class RiskAssessmentController {

    private final RiskAssessmentService assessmentService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public RiskAssessmentController(RiskAssessmentService assessmentService,
                                    UserService userService, JwtUtil jwtUtil) {
        this.assessmentService = assessmentService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/questions")
    public List<Map<String, Object>> questions() {
        return assessmentService.getQuestions();
    }

    @PostMapping("/submit")
    public Map<String, Object> submit(@RequestHeader("Authorization") String authHeader,
                                      @RequestBody Map<String, Object> body) {
        Long userId = getUserId(authHeader);
        // 适当性办法要求投资者签署风险揭示书后才能出具评估结果
        if (!Boolean.TRUE.equals(body.get("agreed"))) {
            throw new RuntimeException("请先阅读并确认《风险揭示书》");
        }
        List<Integer> answers = new ArrayList<>();
        Object raw = body.get("answers");
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Number n) answers.add(n.intValue());
                else throw new RuntimeException("答案格式不正确");
            }
        }
        return assessmentService.submit(userId, answers);
    }

    @GetMapping("/history")
    public IPage<RiskAssessment> history(@RequestHeader("Authorization") String authHeader,
                                         @RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = getUserId(authHeader);
        return assessmentService.getHistory(userId, pageNum, pageSize);
    }

    @GetMapping("/latest")
    public Map<String, Object> latest(@RequestHeader("Authorization") String authHeader) {
        Long userId = getUserId(authHeader);
        RiskAssessment latest = assessmentService.getLatest(userId);
        if (latest == null) return Map.of("exists", false);
        return assessmentService.describeRecord(latest);
    }

    private Long getUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.getUsernameFromToken(token);
        User user = userService.findByUsername(username);
        return user.getId();
    }
}