package com.investedu.smartassistant.agent;

import com.investedu.smartassistant.mcp.resource.PortfolioResource;
import com.investedu.smartassistant.mcp.resource.RiskProfileResource;
import com.investedu.smartassistant.retriever.HybridRetriever;
import com.investedu.smartassistant.service.QueryRewriter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AgentSteps {

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private HybridRetriever hybridRetriever;

    @Autowired
    private QueryRewriter queryRewriter;

    // 注入 MCP 风格资源
    @Autowired
    @Qualifier("mcp_portfolio_resource")
    private PortfolioResource portfolioResource;

    @Autowired
    @Qualifier("mcp_risk_profile_resource")
    private RiskProfileResource riskProfileResource;

    public AgentState riskAssessment(AgentState state) {
        // 使用 MCP 资源获取风险等级和持仓
        String riskProfile = riskProfileResource.read();
        String portfolio = portfolioResource.read();

        if (riskProfile != null && !riskProfile.isEmpty()) {
            state.getContext().put("riskLevel", riskProfile);
        } else {
            // 降级：用大模型评估
            String prompt = "请根据用户输入评估其风险承受能力，返回风险等级（保守/稳健/积极）。用户：" + state.getUserInput();
            String level = chatLanguageModel.generate(prompt);
            state.getContext().put("riskLevel", level.trim());
        }
        state.getContext().put("portfolio", portfolio);
        state.setCurrentStep("rule_lookup");
        return state;
    }

    public AgentState ruleLookup(AgentState state) {
        String rewritten = queryRewriter.rewrite(state.getUserInput());
        List<TextSegment> segments = hybridRetriever.retrieve(rewritten, 3);
        String rules = segments.stream()
                .map(TextSegment::text)
                .collect(Collectors.joining("\n"));
        state.getContext().put("rules", rules);
        state.setCurrentStep("simulate_trade");
        return state;
    }

    public AgentState simulateTrade(AgentState state) {
        String prompt = String.format(
                "根据以下信息为用户生成模拟交易指引：\n风险等级：%s\n持仓：%s\n相关规则：%s\n用户问题：%s",
                state.getContext().get("riskLevel"),
                state.getContext().getOrDefault("portfolio", "无"),
                state.getContext().get("rules"),
                state.getUserInput()
        );
        String advice = chatLanguageModel.generate(prompt);
        state.getContext().put("advice", advice);
        state.setCurrentStep("generate_report");
        return state;
    }

    public AgentState generateReport(AgentState state) {
        String report = "风险等级：" + state.getContext().get("riskLevel") +
                "\n相关规则：" + state.getContext().get("rules") +
                "\n模拟建议：" + state.getContext().get("advice");
        state.getContext().put("report", report);
        return state;
    }
}