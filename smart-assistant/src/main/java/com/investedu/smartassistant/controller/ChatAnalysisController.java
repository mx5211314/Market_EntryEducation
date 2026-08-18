package com.investedu.smartassistant.controller;

import com.investedu.smartassistant.service.ChatAnalysisService;
import com.investedu.smartassistant.service.ChatSessionService;
import com.investedu.smartassistant.util.AuthContext;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatAnalysisController {

    private final ChatAnalysisService chatAnalysisService;
    private final ChatSessionService chatSessionService;
    private final AuthContext authContext;

    public ChatAnalysisController(ChatAnalysisService chatAnalysisService,
                                  ChatSessionService chatSessionService,
                                  AuthContext authContext) {
        this.chatAnalysisService = chatAnalysisService;
        this.chatSessionService = chatSessionService;
        this.authContext = authContext;
    }

    /**
     * 分析指定会话的情绪状态
     */
    @GetMapping("/analyze-session/{sessionId}")
    public Map<String, Object> analyzeSession(@PathVariable String sessionId) {
        // 分析的是聊天原文，会话不属于自己就不能分析
        chatSessionService.requireOwned(authContext.requireUserId(), sessionId);
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
        chatSessionService.requireOwned(authContext.requireUserId(), sessionId);
        return chatAnalysisService.analyzeSession(sessionId);
    }
}
