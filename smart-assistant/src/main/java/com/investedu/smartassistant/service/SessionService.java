package com.investedu.smartassistant.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    // 会话ID -> 消息列表，每条消息是 Map.of("role","user"/"assistant", "content","...")
    private final Map<String, List<Map<String, String>>> sessions = new ConcurrentHashMap<>();

    // 最多保留最近 N 条历史
    private static final int MAX_HISTORY = 10;

    public List<Map<String, String>> getHistory(String sessionId) {
        return sessions.getOrDefault(sessionId, new ArrayList<>());
    }

    public void addMessage(String sessionId, String role, String content) {
        sessions.computeIfAbsent(sessionId, k -> new ArrayList<>())
                .add(Map.of("role", role, "content", content));
        // 保持历史长度
        List<Map<String, String>> history = sessions.get(sessionId);
        if (history.size() > MAX_HISTORY * 2) { // 一问一答算两条
            history.subList(0, history.size() - MAX_HISTORY * 2).clear();
        }
    }

    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
    }
}