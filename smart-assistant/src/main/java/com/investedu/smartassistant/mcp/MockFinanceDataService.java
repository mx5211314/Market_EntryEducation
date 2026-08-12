package com.investedu.smartassistant.mcp;
// 模拟MCP数据服务
import org.springframework.stereotype.Service;

@Service
public class MockFinanceDataService {

    public String getPortfolio() {
        return "持仓：贵州茅台(600519) 1000股，当前价 1800.00元；宁德时代(300750) 500股，当前价 210.00元。";
    }

    public String getRiskProfile() {
        return "积极型，可承受较高风险，适合参与融资融券、科创板等业务。";
    }

    public String getMarketQuote(String symbol) {
        return symbol + " 最新价：1850.00元，涨跌幅：+2.3%";
    }
}