package com.investedu.smartassistant.mcp.resource;

import org.springframework.stereotype.Component;

/**
 * MCP 资源：实时行情
 * 协议：market://quote/{symbol}
 */
@Component("mcp_market_quote_resource")
public class MarketQuoteResource {

    public String read(String symbol) {
        return symbol + " 最新价：1850.00元，涨跌幅：+2.3%，成交量：123456手";
    }
}