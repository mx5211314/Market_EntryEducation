package com.investedu.smartassistant.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Aspect
@Component
public class ChatMetricsAspect {

    @Around("execution(* com.investedu.smartassistant.controller.ChatController.chat(..))")
    public Object logChatMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        // 提取输入
        Object[] args = joinPoint.getArgs();
        String userMessage = args.length > 0 ? ((Map<String, String>) args[0]).get("message") : "";

        Object result = joinPoint.proceed();
        long totalTime = System.currentTimeMillis() - start;

        // 提取回复长度和法规引用
        if (result instanceof Map) {
            String reply = (String) ((Map<?, ?>) result).get("reply");
            int replyLength = reply != null ? reply.length() : 0;
            String ruleIds = com.investedu.smartassistant.service.ResponseValidator.extractRuleIds(
                    reply != null ? reply : ""
            );

            log.info("📊 同步问答监控 | 输入长度: {} | 回复长度: {} | 总耗时: {}ms | 引用法规: {}",
                    userMessage.length(), replyLength, totalTime, ruleIds);
        }

        return result;
    }

    @Around("execution(* com.investedu.smartassistant.controller.ChatController.chatStream(..))")
    public Object logStreamMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long totalTime = System.currentTimeMillis() - start;

        log.info("📊 流式问答监控 | 总耗时: {}ms (流式连接已建立)", totalTime);
        return result;
    }

    @Around("execution(* com.investedu.smartassistant.controller.ChatController.agentGuidance(..))")
    public Object logAgentGuidance(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long totalTime = System.currentTimeMillis() - start;

        String userInput = "";
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Map) {
            Object msg = ((Map<?, ?>) args[0]).get("message");
            if (msg != null) userInput = msg.toString();
        }

        if (result instanceof Map) {
            String guidance = (String) ((Map<?, ?>) result).get("guidance");
            int len = guidance != null ? guidance.length() : 0;
            log.info("🤖 Agent引导监控 | 输入长度: {} | 回复长度: {} | 总耗时: {}ms",
                    userInput.length(), len, totalTime);
        }

        return result;
    }
}