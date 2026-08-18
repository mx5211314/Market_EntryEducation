package com.investedu.smartassistant.controller;

import com.investedu.smartassistant.entity.ChatMessage;
import com.investedu.smartassistant.entity.ChatSession;
import com.investedu.smartassistant.service.ChatMessageService;
import com.investedu.smartassistant.service.ChatSessionService;
import com.investedu.smartassistant.util.AuthContext;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/session")
public class ChatSessionController {

    private final ChatSessionService sessionService;
    private final ChatMessageService messageService;
    private final AuthContext authContext;

    public ChatSessionController(ChatSessionService sessionService,
                                 ChatMessageService messageService,
                                 AuthContext authContext) {
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.authContext = authContext;
    }

    // 创建新会话
    @PostMapping("/create")
    public ChatSession createSession(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "新对话");
        return sessionService.createSession(authContext.requireUserId(), title);
    }

    // 获取当前用户的会话列表
    @GetMapping("/list")
    public List<ChatSession> listSessions() {
        return sessionService.listSessions(authContext.requireUserId());
    }

    // 获取指定会话的全部消息
    @GetMapping("/{sessionId}/messages")
    public List<ChatMessage> listMessages(@PathVariable String sessionId) {
        // 之前只按 sessionId 查消息，换个 id 就能翻别人的对话
        sessionService.requireOwned(authContext.requireUserId(), sessionId);
        return messageService.listMessages(sessionId);
    }

    // 删除会话（同时删除消息）
    @DeleteMapping("/{sessionId}")
    public Map<String, String> deleteSession(@PathVariable String sessionId) {
        sessionService.deleteSession(authContext.requireUserId(), sessionId);
        return Map.of("message", "删除成功");
    }

    // 重命名会话
    @PutMapping("/{sessionId}/title")
    public Map<String, String> renameSession(@PathVariable String sessionId,
                                             @RequestBody Map<String, String> body) {
        boolean ok = sessionService.renameSession(authContext.requireUserId(), sessionId, body.get("title"));
        return Map.of("message", ok ? "重命名成功" : "重命名失败");
    }
}
