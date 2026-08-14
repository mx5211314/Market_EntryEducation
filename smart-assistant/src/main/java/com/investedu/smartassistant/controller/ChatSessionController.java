package com.investedu.smartassistant.controller;

import com.investedu.smartassistant.entity.ChatMessage;
import com.investedu.smartassistant.entity.ChatSession;
import com.investedu.smartassistant.entity.User;
import com.investedu.smartassistant.service.ChatMessageService;
import com.investedu.smartassistant.service.ChatSessionService;
import com.investedu.smartassistant.service.UserService;
import com.investedu.smartassistant.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/session")
public class ChatSessionController {

    private final ChatSessionService sessionService;
    private final ChatMessageService messageService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public ChatSessionController(ChatSessionService sessionService,
                                 ChatMessageService messageService,
                                 UserService userService,
                                 JwtUtil jwtUtil) {
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    // 创建新会话
    @PostMapping("/create")
    public ChatSession createSession(@RequestHeader("Authorization") String authHeader,
                                     @RequestBody Map<String, String> body) {
        User user = getCurrentUser(authHeader);
        String title = body.getOrDefault("title", "新对话");
        return sessionService.createSession(user.getId(), title);
    }

    // 获取当前用户的会话列表
    @GetMapping("/list")
    public List<ChatSession> listSessions(@RequestHeader("Authorization") String authHeader) {
        User user = getCurrentUser(authHeader);
        return sessionService.listSessions(user.getId());
    }

    // 获取指定会话的全部消息
    @GetMapping("/{sessionId}/messages")
    public List<ChatMessage> listMessages(@RequestHeader("Authorization") String authHeader,
                                          @PathVariable String sessionId) {
        return messageService.listMessages(sessionId);
    }

    // 删除会话（同时删除消息）
    @DeleteMapping("/{sessionId}")
    public Map<String, String> deleteSession(@RequestHeader("Authorization") String authHeader,
                                             @PathVariable String sessionId) {
        User user = getCurrentUser(authHeader);
        sessionService.deleteSession(user.getId(), sessionId);
        return Map.of("message", "删除成功");
    }

    private User getCurrentUser(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.getUsernameFromToken(token);
        return userService.findByUsername(username);
    }
}