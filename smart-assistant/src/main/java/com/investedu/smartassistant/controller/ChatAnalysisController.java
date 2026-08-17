package com.investedu.smartassistant.controller;

import com.investedu.smartassistant.service.ChatAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatAnalysisController {

    @Autowired
    private ChatAnalysisService chatAnalysisService;

    /**
     * 分析指定会话的情绪状态
     */
    @GetMapping("/analyze-session/{sessionId}")
    public Map<String, Object> analyzeSession(@PathVariable String sessionId) {
        return chatAnalysisService.analyzeSession(sessionId);
    }

    /**
     * 分析指定会话的情绪状态（POST 方式）
     */
    @PostMapping("/analyze-session")
    public Map<String, Object> analyzeSessionByPost(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        if (sessionId == null || sessionId.isEmpty()) {
            return chatAnalysisService.createDefaultAnalysis();
        }
        return chatAnalysisService.analyzeSession(sessionId);
    }
}
