package com.investedu.smartassistant.mcp.resource;

import org.springframework.stereotype.Component;

/**
 * MCP 资源：用户持仓数据
 * 协议：user://portfolio
 * 生产环境接入真实 MCP Server，此处为模拟实现
 */
@Component("mcp_portfolio_resource")
public class PortfolioResource {

    /**
     * MCP Read 操作，返回用户当前持仓信息
     */
    public String read() {
        return "持仓：贵州茅台(600519) 1000股，当前价 1800.00元；" +
                "宁德时代(300750) 500股，当前价 210.00元。";
    }
}