package com.investedu.smartassistant.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;
// Agent 状态
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AgentState {
    private String currentStep; // RISK_ASSESSMENT, RULE_LOOKUP, SIMULATE, REPORT
    private String userInput;
    private Map<String, Object> context; // 存储风险等级、检索结果等
}