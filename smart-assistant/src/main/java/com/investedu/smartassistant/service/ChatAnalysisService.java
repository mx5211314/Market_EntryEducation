package com.investedu.smartassistant.service;

import com.investedu.smartassistant.entity.ChatMessage;
import com.investedu.smartassistant.mapper.ChatMessageMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatAnalysisService {

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    private static final String ANALYSIS_PROMPT =
            "你是一位专业的投资心理辅导助手。请分析用户的投资对话内容，评估其情绪状态和投资风险倾向。\n" +
            "请严格按照以下 JSON 格式返回（不要包含 markdown 格式）：\n" +
            "{\n" +
            "  \"primaryEmotion\": \"主要情绪（焦虑/兴奋/冷静/困惑/犹豫/贪婪/恐惧）\",\n" +
            "  \"emotionScore\": 情绪强度分数（0-100 整数），\n" +
            "  \"isNegative\": 是否为负面情绪（true/false），\n" +
            "  \"riskLevel\": 风险等级（0-3 整数，0 正常/1 关注/2 预警/3 危机），\n" +
            "  \"riskDescription\": \"风险描述（一句话说明用户的风险状态）\",\n" +
            "  \"suggestion\": \"给用户的投资建议（温暖、鼓励的语气）\",\n" +
            "  \"improvementSuggestions\": [\"建议 1\", \"建议 2\", \"建议 3\"]\n" +
            "}\n\n" +
            "用户对话内容：";

    /**
     * 分析会话中的用户情绪
     * @param sessionId 会话 ID
     * @return 分析结果 Map
     */
    public Map<String, Object> analyzeSession(String sessionId) {
        // 获取会话中用户的最后几条消息
        List<ChatMessage> messages = chatMessageMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatMessage>()
                .eq("session_id", sessionId)
                .eq("role", "user")
                .orderByDesc("created_at")
                .last("LIMIT 5")
        );

        if (messages.isEmpty()) {
            return createDefaultAnalysis();
        }

        // 拼接用户消息
        StringBuilder userContent = new StringBuilder();
        for (int i = messages.size() - 1; i >= 0; i--) {
            userContent.append(messages.get(i).getContent()).append("\n");
        }

        try {
            String response = chatLanguageModel.generate(ANALYSIS_PROMPT + userContent.toString());
            return parseAnalysisResponse(response);
        } catch (Exception e) {
            e.printStackTrace();
            return createDefaultAnalysis();
        }
    }

    /**
     * 分析文本情绪（通用方法）
     */
    public Map<String, Object> analyzeText(String text) {
        try {
            String response = chatLanguageModel.generate(ANALYSIS_PROMPT + text);
            return parseAnalysisResponse(response);
        } catch (Exception e) {
            return createDefaultAnalysis();
        }
    }

    /**
     * 解析 AI 返回的 JSON 响应
     */
    private Map<String, Object> parseAnalysisResponse(String response) {
        Map<String, Object> result = new HashMap<>();

        // 默认值
        result.put("primaryEmotion", "平静");
        result.put("emotionScore", 50);
        result.put("isNegative", false);
        result.put("riskLevel", 0);
        result.put("riskDescription", "情绪状态平稳");
        result.put("suggestion", "保持当前的投资心态，继续学习成长");
        result.put("improvementSuggestions", java.util.Arrays.asList("定期复盘", "持续学习", "理性决策"));

        try {
            // 清理可能的 markdown 标记
            String jsonStr = response.trim();
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("\n") + 1);
                if (jsonStr.endsWith("```")) {
                    jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
                }
            }
            jsonStr = jsonStr.trim();

            // 使用 Jackson 解析（Spring Boot 自带）
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> parsed = mapper.readValue(jsonStr, Map.class);

            if (parsed.containsKey("primaryEmotion")) {
                result.put("primaryEmotion", parsed.get("primaryEmotion"));
            }
            if (parsed.containsKey("emotionScore")) {
                result.put("emotionScore", ((Number) parsed.get("emotionScore")).intValue());
            }
            if (parsed.containsKey("isNegative")) {
                result.put("isNegative", (Boolean) parsed.get("isNegative"));
            }
            if (parsed.containsKey("riskLevel")) {
                result.put("riskLevel", ((Number) parsed.get("riskLevel")).intValue());
            }
            if (parsed.containsKey("riskDescription")) {
                result.put("riskDescription", (String) parsed.get("riskDescription"));
            }
            if (parsed.containsKey("suggestion")) {
                result.put("suggestion", (String) parsed.get("suggestion"));
            }
            if (parsed.containsKey("improvementSuggestions")) {
                result.put("improvementSuggestions", parsed.get("improvementSuggestions"));
            }
        } catch (Exception e) {
            // 解析失败时返回默认值
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 创建默认分析结果（用于 AI 不可用时）
     */
    public Map<String, Object> createDefaultAnalysis() {
        Map<String, Object> result = new HashMap<>();
        result.put("primaryEmotion", "平静");
        result.put("emotionScore", 50);
        result.put("isNegative", false);
        result.put("riskLevel", 0);
        result.put("riskDescription", "情绪状态平稳");
        result.put("suggestion", "保持当前的投资心态，继续学习成长");
        result.put("improvementSuggestions", java.util.Arrays.asList("定期复盘", "持续学习", "理性决策"));
        return result;
    }
}
