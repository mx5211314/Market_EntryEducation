package com.investedu.smartassistant.mcp.resource;

import org.springframework.stereotype.Component;

/**
 * MCP 资源：用户风险测评结果
 * 协议：user://risk_profile
 */
@Component("mcp_risk_profile_resource")
public class RiskProfileResource {

    public String read() {
        return "积极型，可承受较高风险，适合参与融资融券、科创板等业务。";
    }
}
