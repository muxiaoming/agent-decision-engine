package com.zhou.ai.tools.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 股价查询工具 - 获取实时股票价格和历史数据。
 */
@Service
public class StockPriceToolService {

    /**
     * 查询股票实时价格。
     *
     * @param symbol 股票代码（如 AAPL, MSFT, GOOGL）
     * @return 包含股价、涨跌幅等信息
     */
    @Tool(description = "查询股票实时价格，返回当前价格、涨跌幅、成交量等信息")
    public Map<String, Object> getStockPrice(
            @ToolParam(description = "股票代码，如 AAPL（苹果）、MSFT（微软）、GOOGL（谷歌）") String symbol
    ) {
        // 模拟股票数据
        Map<String, Object> stockData = switch (symbol.toUpperCase()) {
            case "AAPL" -> Map.of(
                    "symbol", "AAPL",
                    "name", "Apple Inc.",
                    "price", 178.52,
                    "change", 2.35,
                    "changePercent", 1.33,
                    "volume", 52345678,
                    "marketCap", "2.8T"
            );
            case "MSFT" -> Map.of(
                    "symbol", "MSFT",
                    "name", "Microsoft Corporation",
                    "price", 412.89,
                    "change", -1.23,
                    "changePercent", -0.30,
                    "volume", 23456789,
                    "marketCap", "3.1T"
            );
            case "GOOGL" -> Map.of(
                    "symbol", "GOOGL",
                    "name", "Alphabet Inc.",
                    "price", 175.98,
                    "change", 3.45,
                    "changePercent", 2.00,
                    "volume", 18765432,
                    "marketCap", "2.2T"
            );
            default -> Map.of(
                    "symbol", symbol.toUpperCase(),
                    "name", "Unknown Stock",
                    "price", 0.0,
                    "error", "股票代码不存在或暂不支持"
            );
        };

        return stockData;
    }

    /**
     * 查询股票历史价格。
     *
     * @param symbol 股票代码
     * @param days   查询天数（最近N天）
     * @return 历史价格数据
     */
    @Tool(description = "查询股票历史价格，返回最近N天的价格走势")
    public Map<String, Object> getStockHistory(
            @ToolParam(description = "股票代码") String symbol,
            @ToolParam(description = "查询天数，如 7、30、90") int days
    ) {
        // 模拟历史数据
        return Map.of(
                "symbol", symbol.toUpperCase(),
                "period", days + "天",
                "data", Map.of(
                        "startPrice", 175.00,
                        "endPrice", 178.52,
                        "highestPrice", 180.25,
                        "lowestPrice", 173.50,
                        "averagePrice", 176.89,
                        "totalChange", 2.01
                )
        );
    }

    /**
     * 计算股票收益率。
     *
     * @param symbol   股票代码
     * @param buyPrice 买入价格
     * @param shares   持股数量
     * @return 收益率和收益金额
     */
    @Tool(description = "计算股票投资收益率，包括收益率和收益金额")
    public Map<String, Object> calculateReturn(
            @ToolParam(description = "股票代码") String symbol,
            @ToolParam(description = "买入价格") double buyPrice,
            @ToolParam(description = "持股数量") int shares
    ) {
        String normalizedSymbol = symbol.toUpperCase();
        double currentPrice = lookupCurrentPrice(normalizedSymbol);

        double totalCost = buyPrice * shares;
        double currentValue = currentPrice * shares;
        double profit = currentValue - totalCost;
        double returnRate = (profit / totalCost) * 100;

        return Map.of(
                "symbol", normalizedSymbol,
                "buyPrice", buyPrice,
                "currentPrice", currentPrice,
                "shares", shares,
                "totalCost", String.format("%.2f", totalCost),
                "currentValue", String.format("%.2f", currentValue),
                "profit", String.format("%.2f", profit),
                "returnRate", String.format("%.2f%%", returnRate)
        );
    }

    private double lookupCurrentPrice(String normalizedSymbol) {
        return switch (normalizedSymbol) {
            case "AAPL" -> 178.52;
            case "MSFT" -> 412.89;
            case "GOOGL" -> 175.98;
            default -> 0.0;
        };
    }
}
