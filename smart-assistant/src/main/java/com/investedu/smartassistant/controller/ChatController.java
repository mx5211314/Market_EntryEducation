package com.investedu.smartassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investedu.smartassistant.agent.InvestmentAgent;
import com.investedu.smartassistant.entity.ChatMessage;
import com.investedu.smartassistant.entity.ChatSession;
import com.investedu.smartassistant.entity.User;
import com.investedu.smartassistant.retriever.HybridRetriever;
import com.investedu.smartassistant.service.ChatMessageService;
import com.investedu.smartassistant.service.ChatSessionService;
import com.investedu.smartassistant.service.QueryRewriter;
import com.investedu.smartassistant.service.ResponseValidator;
import com.investedu.smartassistant.service.UserService;
import com.investedu.smartassistant.util.JwtUtil;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ChatController {

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private HybridRetriever hybridRetriever;

    @Autowired
    private InvestmentAgent investmentAgent;

    @Autowired
    private QueryRewriter queryRewriter;

    @Autowired
    private ResponseValidator responseValidator;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.stream-url}")
    private String streamUrl;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String modelName;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
            "你是“入市教育智慧助手”，一个由金融法规知识驱动的智能伴学系统。\n" +
                    "你的使命是帮助投资者理解证券交易规则、融资融券业务、风险测评等金融知识，并引导他们通过模拟交易熟悉市场。\n" +
                    "回答风格请参考 DeepSeek：结构化、条理清晰、详细且友好。如果问题与金融无关，可以简短聊天，但最终引导回投资教育主题。\n" +
                    "回答中必须包含“风险提示”和“法规来源”（如果引用了知识库）。\n" +
                    "不要编造知识库中没有的法规，如果知识库无相关内容，请基于通用金融知识回答，并注明“仅供参考”。";

    /**
     * 获取会话列表
     */
    @GetMapping("/chat")
    public List<Map<String, Object>> getSessionList(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("未登录");
        }
        String token = authHeader.substring(7);
        String username = jwtUtil.getUsernameFromToken(token);
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        List<ChatSession> sessions = chatSessionService.listSessions(user.getId());
        return sessions.stream().map(session -> {
            Map<String, Object> map = new HashMap<>();
            map.put("sessionId", session.getSessionId());
            map.put("title", session.getTitle());
            map.put("createdAt", session.getCreatedAt());
            map.put("updatedAt", session.getUpdatedAt());
            // 获取最后一条消息
            List<ChatMessage> messages = chatMessageService.listMessages(session.getSessionId());
            if (!messages.isEmpty()) {
                ChatMessage lastMsg = messages.get(messages.size() - 1);
                map.put("lastMessage", lastMsg.getContent());
            }
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 同步问答接口（支持持久化会话）
     */
    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> request,
                                    @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String sessionId = request.getOrDefault("sessionId", "");
        String userMessage = request.get("message");
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return Map.of("reply", "您好，请问有什么可以帮您的？");
        }

        // 获取当前用户
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("未登录");
        }
        String token = authHeader.substring(7);
        String username = jwtUtil.getUsernameFromToken(token);
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        Long userId = user.getId();

        // 创建或更新会话
        if (sessionId.isEmpty()) {
            ChatSession newSession = chatSessionService.createSession(userId, userMessage.substring(0, Math.min(20, userMessage.length())));
            sessionId = newSession.getSessionId();
        } else {
            chatSessionService.touchSession(userId, sessionId);
        }

        // 保存用户消息
        chatMessageService.saveMessage(sessionId, userId, "user", userMessage);

        // 获取历史消息
        List<Map<String, String>> history = chatMessageService.listMessages(sessionId).stream()
                .map(msg -> Map.of("role", msg.getRole(), "content", msg.getContent()))
                .collect(Collectors.toList());

        // 意图重写与回答生成
        String rewritten = queryRewriter.rewrite(userMessage);
        String reply;
        if (QueryRewriter.isChat(rewritten)) {
            reply = chatWithHistory(history, userMessage);
        } else {
            List<TextSegment> relevant = hybridRetriever.retrieve(rewritten, 4);
            String context = relevant.stream().map(TextSegment::text).collect(Collectors.joining("\n\n"));
            reply = responseValidator.generateWithValidation(buildFinancePrompt(context, userMessage, history), 2);
        }

        // 保存助手消息
        chatMessageService.saveMessage(sessionId, userId, "assistant", reply);

        return Map.of("reply", reply, "sessionId", sessionId);
    }

    /**
     * 流式问答接口（SSE，支持持久化会话）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, String> request,
                                 @RequestHeader(value = "Authorization", required = false) String authHeader) {
        SseEmitter emitter = new SseEmitter(0L);
        String sessionId = request.getOrDefault("sessionId", "");
        String userMessage = request.get("message");
        if (userMessage == null || userMessage.trim().isEmpty()) {
            sendSingleSseEvent(emitter, "您好，请问有什么可以帮您的？");
            return emitter;
        }

        // 获取当前用户
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            emitter.completeWithError(new RuntimeException("未登录"));
            return emitter;
        }
        String token = authHeader.substring(7);
        String username = jwtUtil.getUsernameFromToken(token);
        User user = userService.findByUsername(username);
        if (user == null) {
            emitter.completeWithError(new RuntimeException("用户不存在"));
            return emitter;
        }
        Long userId = user.getId();

        // 创建或更新会话
        if (sessionId.isEmpty()) {
            ChatSession newSession = chatSessionService.createSession(userId, userMessage.substring(0, Math.min(20, userMessage.length())));
            sessionId = newSession.getSessionId();
        } else {
            chatSessionService.touchSession(userId, sessionId);
        }

        // 保存用户消息
        chatMessageService.saveMessage(sessionId, userId, "user", userMessage);

        // 获取历史消息
        List<Map<String, String>> history = chatMessageService.listMessages(sessionId).stream()
                .map(msg -> Map.of("role", msg.getRole(), "content", msg.getContent()))
                .collect(Collectors.toList());

        // 为 lambda 创建 final 副本
        final String finalSessionId = sessionId;
        final Long finalUserId = userId;
        final String finalUserMessage = userMessage;
        final List<Map<String, String>> finalHistory = history;

        CompletableFuture.runAsync(() -> {
            try {
                String rewritten = queryRewriter.rewrite(finalUserMessage);

                // 闲聊：直接生成并一次性发送
                if (QueryRewriter.isChat(rewritten)) {
                    String reply = chatWithHistory(finalHistory, finalUserMessage);
                    chatMessageService.saveMessage(finalSessionId, finalUserId, "assistant", reply);
                    sendSingleSseEvent(emitter, reply);
                    return;
                }

                // 金融问题：检索知识库，构建流式 API 的 messages
                List<TextSegment> relevant = hybridRetriever.retrieve(rewritten, 4);
                String context = relevant.stream().map(TextSegment::text).collect(Collectors.joining("\n\n"));

                List<Map<String, Object>> messages = new ArrayList<>();
                messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
                for (Map<String, String> msg : finalHistory) {
                    messages.add(Map.of("role", msg.get("role"), "content", msg.get("content")));
                }
                String userPrompt = String.format(
                        "根据以下金融法规知识，回答用户问题。回答中必须包含“风险提示”和“法规来源”。\n\n知识库：\n%s\n\n用户：%s",
                        context.isEmpty() ? "无相关知识" : context,
                        finalUserMessage);
                messages.add(Map.of("role", "user", "content", userPrompt));

                // 调用百炼流式 API
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + apiKey);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", modelName);
                requestBody.put("stream", true);
                requestBody.put("messages", messages);

                restTemplate.execute(streamUrl, HttpMethod.POST, req -> {
                    req.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    req.getHeaders().set("Authorization", "Bearer " + apiKey);
                    req.getBody().write(objectMapper.writeValueAsString(requestBody).getBytes(StandardCharsets.UTF_8));
                }, resp -> {
                    StringBuilder fullReply = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(resp.getBody(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6).trim();
                                if ("[DONE]".equals(data)) break;
                                try {
                                    Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                                    List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                                    if (choices != null && !choices.isEmpty()) {
                                        Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                                        if (delta != null && delta.containsKey("content")) {
                                            String content = (String) delta.get("content");
                                            if (content != null && !content.isEmpty()) {
                                                fullReply.append(content);
                                                emitter.send(SseEmitter.event().data(content));
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    // 忽略解析错误
                                }
                            }
                        }
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                        return null;
                    }
                    // 保存助手消息
                    chatMessageService.saveMessage(finalSessionId, finalUserId, "assistant", fullReply.toString());
                    emitter.complete();
                    return null;
                });
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * Agent 引导接口（同步）
     */
    @PostMapping("/agent/guidance")
    public Map<String, String> agentGuidance(@RequestBody Map<String, String> request) {
        String userInput = request.get("message");
        if (userInput == null || userInput.trim().length() < 2) {
            return Map.of("guidance", "请输入完整的问题，例如“我想了解融资融券交易规则”。");
        }
        String report = investmentAgent.run(userInput);
        return Map.of("guidance", report);
    }

    // ==================== 私有辅助方法 ====================

    private void sendSingleSseEvent(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().data(message));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    private String buildFinancePrompt(String context, String currentMsg, List<Map<String, String>> history) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYSTEM_PROMPT).append("\n\n");
        sb.append("历史对话：\n");
        for (Map<String, String> msg : history) {
            sb.append(msg.get("role")).append(": ").append(msg.get("content")).append("\n");
        }
        sb.append("当前问题：").append(currentMsg).append("\n");
        sb.append("知识库参考：\n").append(context.isEmpty() ? "无" : context).append("\n");
        sb.append("assistant: ");
        return sb.toString();
    }

    private String chatWithHistory(List<Map<String, String>> history, String currentMsg) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYSTEM_PROMPT).append("\n\n");
        for (Map<String, String> msg : history) {
            sb.append(msg.get("role")).append(": ").append(msg.get("content")).append("\n");
        }
        sb.append("user: ").append(currentMsg).append("\nassistant: ");
        return chatLanguageModel.generate(sb.toString()).trim();
    }
}