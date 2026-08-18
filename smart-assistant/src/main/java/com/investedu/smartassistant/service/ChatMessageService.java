package com.investedu.smartassistant.service;

import com.investedu.smartassistant.entity.ChatMessage;
import com.investedu.smartassistant.mapper.ChatMessageMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatMessageService {

    private final ChatMessageMapper messageMapper;

    public ChatMessageService(ChatMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    public void saveMessage(String sessionId, Long userId, String role, String content) {
        saveMessage(sessionId, userId, role, content, null);
    }

    public void saveMessage(String sessionId, Long userId, String role, String content, String sourcesJson) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        message.setSources(sourcesJson);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
    }

    public List<ChatMessage> listMessages(String sessionId) {
        return messageMapper.listBySessionId(sessionId);
    }
}