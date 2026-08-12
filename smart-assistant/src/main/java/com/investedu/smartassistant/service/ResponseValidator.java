package com.investedu.smartassistant.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResponseValidator {

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    // 高风险词拦截
    private static final Set<String> BLOCKED_WORDS = Set.of(
            "保本", "稳赚", "内幕消息", "必涨", "无风险", "零风险"
    );

    // 法规编号模式：《XXX》第XX条
    private static final Pattern RULE_PATTERN = Pattern.compile("《[^》]+》第[\\d一二三四五六七八九十百]+条");

    /**
     * 生成回复，并进行合规校验，若失败则自动重试
     */
    public String generateWithValidation(String prompt, int maxRetries) {
        String currentPrompt = prompt;
        for (int i = 0; i <= maxRetries; i++) {
            String response = chatLanguageModel.generate(currentPrompt);

            // 1. 检查高风险词
            if (containsBlockedWords(response)) {
                currentPrompt = prompt + "\n\n【重要】上一轮回答包含不适当表述（如“保本”“稳赚”），请严格按照金融合规要求，使用客观、审慎的语言重新回答。";
                continue;
            }

            // 2. 检查是否包含风险提示和法规来源
            if (!validateRequiredFields(response)) {
                currentPrompt = prompt + "\n\n【重要】上一轮回答缺少必填项（风险提示或法规来源），请务必包含“风险提示”和“法规来源”字段。";
                continue;
            }

            return response;
        }
        // 兜底回复
        return "根据相关金融法规，我们暂时无法生成完整回答，请稍后重试。\n\n风险提示：股市有风险，投资需谨慎。\n法规来源：请参阅中国证监会及证券交易所相关规则。";
    }

    // 检查必填字段
    private boolean validateRequiredFields(String response) {
        boolean hasRisk = response.contains("风险提示") || response.contains("风险");
        boolean hasSource = response.contains("法规来源") || response.contains("《");
        return hasRisk && hasSource;
    }

    // 检查高风险词
    private boolean containsBlockedWords(String response) {
        for (String word : BLOCKED_WORDS) {
            if (response.contains(word)) return true;
        }
        return false;
    }

    // 提取法规编号（可对外暴露，供监控使用）
    public static String extractRuleIds(String response) {
        Matcher matcher = RULE_PATTERN.matcher(response);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(matcher.group());
        }
        return sb.toString();
    }
}