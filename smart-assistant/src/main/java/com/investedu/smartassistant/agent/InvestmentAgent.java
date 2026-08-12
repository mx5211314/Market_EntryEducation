package com.investedu.smartassistant.agent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class InvestmentAgent {

    @Autowired
    private AgentSteps steps;

    public String run(String userInput) {
        AgentState state = new AgentState();
        state.setUserInput(userInput);
        state.setCurrentStep("risk_assessment");
        state.setContext(new HashMap<>());

        // 按顺序执行流程
        steps.riskAssessment(state);
        steps.ruleLookup(state);
        steps.simulateTrade(state);
        steps.generateReport(state);

        return state.getContext().get("report").toString();
    }
}