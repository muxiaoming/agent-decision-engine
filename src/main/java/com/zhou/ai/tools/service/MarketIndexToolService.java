package com.zhou.ai.tools.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 市场指标工具 - 获取大盘指数和市场整体指标。
 */
@Service
public class MarketIndexToolService {

    /**
     * 查询大盘指数。
     *
     * @param indexName 指数名称（如 上证指数、深成指、创业板）
     * @return 指数数据
     */
    @Tool(description = "查询大盘指数，包括上证指数、深成指、创业板指等")
    public Map<String, Object> getMarketIndex(
            @ToolParam(description = "指数名称，如 上证指数、深成指、创业板指、沪深300") String indexName
    ) {
        return switch (indexName) {
            case "上证指数" -> Map.of(
                    "name", "上证指数",
                    "code", "000001.SH",
                    "value", 3056.78,
                    "change", 45.23,
                    "changePercent", 1.50,
                    "volume", "3200亿"
            );
            case "深成指" -> Map.of(
                    "name", "深成指",
                    "code", "399001.SZ",
                    "value", 9543.21,
                    "change", -128.45,
                    "changePercent", -1.33,
                    "volume", "4500亿"
            );
            case "创业板指" -> Map.of(
                    "name", "创业板指",
                    "code", "399006.SZ",
                    "value", 1876.54,
                    "change", 23.67,
                    "changePercent", 1.28,
                    "volume", "1800亿"
            );
            case "沪深300" -> Map.of(
                    "name", "沪深300",
                    "code", "000300.SH",
                    "value", 3698.45,
                    "change", 52.34,
                    "changePercent", 1.44,
                    "volume", "2800亿"
            );
            default -> Map.of(
                    "name", indexName,
                    "error", "指数名称不存在或暂不支持"
            );
        };
    }

    /**
     * 查询市场波动率。
     *
     * @return 市场波动率数据
     */
    @Tool(description = "查询市场波动率（VIX），反映市场风险和波动程度")
    public Map<String, Object> getMarketVolatility() {
        double vixValue = 18.5;

        String riskLevel;
        String description;

        if (vixValue < 15) {
            riskLevel = "低";
            description = "市场情绪稳定，波动较小，适合长期投资";
        } else if (vixValue < 20) {
            riskLevel = "中等";
            description = "市场波动适中，需要关注基本面分析";
        } else if (vixValue < 25) {
            riskLevel = "偏高";
            description = "市场波动较大，建议谨慎投资，控制仓位";
        } else {
            riskLevel = "高";
            description = "市场情绪恐慌，波动剧烈，不建议追高";
        }

        return Map.of(
                "vix", vixValue,
                "riskLevel", riskLevel,
                "description", description,
                "lastUpdate", "2024-01-15 10:30:00"
        );
    }

    /**
     * 查询市场情绪指标。
     *
     * @return 市场情绪数据
     */
    @Tool(description = "查询市场情绪指标，包括涨跌家数、成交量等")
    public Map<String, Object> getMarketSentiment() {
        return Map.of(
                "advancers", 2856,
                "decliners", 1892,
                "unchanged", 234,
                "limitUp", 67,
                "limitDown", 12,
                "totalVolume", "8920亿",
                "sentiment", "偏多",
                "description", "上涨家数多于下跌家数，市场情绪偏乐观"
        );
    }

    /**
     * 查询行业板块表现。
     *
     * @param sector 行业板块名称
     * @return 行业板块数据
     */
    @Tool(description = "查询行业板块表现，包括涨跌幅、领涨股等")
    public Map<String, Object> getSectorPerformance(
            @ToolParam(description = "行业板块名称，如 科技、医药、金融、消费") String sector
    ) {
        return switch (sector) {
            case "科技" -> Map.of(
                    "name", "科技板块",
                    "changePercent", 2.45,
                    "topStocks", new String[]{"阿里巴巴", "腾讯", "华为"},
                    "description", "受AI政策利好，科技板块表现强势"
            );
            case "医药" -> Map.of(
                    "name", "医药板块",
                    "changePercent", -0.89,
                    "topStocks", new String[]{"恒瑞医药", "药明康德", "迈瑞医疗"},
                    "description", "医药板块整体偏弱，部分个股有调整"
            );
            case "金融" -> Map.of(
                    "name", "金融板块",
                    "changePercent", 1.23,
                    "topStocks", new String[]{"工商银行", "招商银行", "中国平安"},
                    "description", "金融板块表现稳定，银行股相对抗跌"
            );
            case "消费" -> Map.of(
                    "name", "消费板块",
                    "changePercent", 0.56,
                    "topStocks", new String[]{"贵州茅台", "五粮液", "伊利股份"},
                    "description", "消费板块小幅上涨，白酒股表现活跃"
            );
            default -> Map.of(
                    "name", sector,
                    "error", "行业板块不存在或暂不支持"
            );
        };
    }
}
