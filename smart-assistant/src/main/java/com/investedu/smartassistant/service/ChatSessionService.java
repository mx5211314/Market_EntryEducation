package com.investedu.smartassistant.service;

import com.investedu.smartassistant.entity.ChatSession;
import com.investedu.smartassistant.mapper.ChatSessionMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ChatSessionService {

    private final ChatSessionMapper sessionMapper;

    public ChatSessionService(ChatSessionMapper sessionMapper) {
        this.sessionMapper = sessionMapper;
    }

    public ChatSession createSession(Long userId, String title) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        session.setTitle(title == null || title.isEmpty() ? "新对话" : title);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.insert(session);
        return session;
    }

    public List<ChatSession> listSessions(Long userId) {
        return sessionMapper.listByUserId(userId);
    }

    public void deleteSession(Long userId, String sessionId) {
        ChatSession session = sessionMapper.findByUserIdAndSessionId(userId, sessionId);
        if (session != null) {
            sessionMapper.deleteById(session.getId());
        }
    }

    public void touchSession(Long userId, String sessionId) {
        ChatSession session = sessionMapper.findByUserIdAndSessionId(userId, sessionId);
        if (session != null) {
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
    }
}