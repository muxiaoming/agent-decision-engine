package com.zhou.ai.tools.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 风险计算工具 - 计算投资组合的风险指标和预期收益。
 */
@Service
public class RiskCalculatorToolService {

    /**
     * 计算投资组合预期收益。
     *
     * @param stockAllocation 股票配置比例（0-100）
     * @param bondAllocation  债券配置比例（0-100）
     * @param cashAllocation  现金配置比例（0-100）
     * @return 预期收益和风险
     */
    @Tool(description = "计算投资组合预期收益，根据股票、债券、现金的配置比例计算年化预期收益")
    public Map<String, Object> calculatePortfolioReturn(
            @ToolParam(description = "股票配置比例（0-100），如 60 表示60%配置股票") int stockAllocation,
            @ToolParam(description = "债券配置比例（0-100），如 30 表示30%配置债券") int bondAllocation,
            @ToolParam(description = "现金配置比例（0-100），如 10 表示10%配置现金") int cashAllocation
    ) {
        // 确保比例之和为100
        int total = stockAllocation + bondAllocation + cashAllocation;
        if (total != 100) {
            return Map.of(
                    "error", "配置比例之和必须为100%，当前为" + total + "%",
                    "stockAllocation", stockAllocation,
                    "bondAllocation", bondAllocation,
                    "cashAllocation", cashAllocation
            );
        }

        // 预期年化收益（模拟）
        double stockReturn = stockAllocation * 0.12;  // 股票预期12%
        double bondReturn = bondAllocation * 0.04;    // 债券预期4%
        double cashReturn = cashAllocation * 0.02;    // 现金预期2%

        double totalReturn = stockReturn + bondReturn + cashReturn;

        RiskAssessment risk = assessRisk(stockAllocation);

        return Map.of(
                "stockAllocation", stockAllocation + "%",
                "bondAllocation", bondAllocation + "%",
                "cashAllocation", cashAllocation + "%",
                "expectedAnnualReturn", String.format("%.2f%%", totalReturn),
                "riskLevel", risk.level(),
                "description", risk.description()
        );
    }

    /**
     * 计算投资组合风险（VaR - 在险价值）。
     *
     * @param investmentAmount  投资金额
     * @param confidenceLevel   置信水平（如 0.95 表示95%）
     * @param holdingPeriodDays 持有天数
     * @return VaR值
     */
    @Tool(description = "计算投资组合的VaR（在险价值），衡量一定置信水平下的最大潜在损失")
    public Map<String, Object> calculateValueAtRisk(
            @ToolParam(description = "投资金额，如 100000 表示10万元") double investmentAmount,
            @ToolParam(description = "置信水平，如 0.95 表示95%") double confidenceLevel,
            @ToolParam(description = "持有天数，如 30 表示30天") int holdingPeriodDays
    ) {
        // 假设年化波动率为20%
        double annualVolatility = 0.20;

        // 计算日波动率
        double dailyVolatility = annualVolatility / Math.sqrt(252);

        // 计算持有期波动率
        double holdingPeriodVolatility = dailyVolatility * Math.sqrt(holdingPeriodDays);

        // 假设Z值（对应95%置信水平为1.645，99%为2.326）
        double zScore = confidenceLevel >= 0.99 ? 2.326 : 1.645;

        // 计算VaR
        double var = investmentAmount * zScore * holdingPeriodVolatility;

        return Map.of(
                "investmentAmount", investmentAmount,
                "confidenceLevel", confidenceLevel * 100 + "%",
                "holdingPeriodDays", holdingPeriodDays + "天",
                "var", String.format("%.2f", var),
                "varPercent", String.format("%.2f%%", (var / investmentAmount) * 100),
                "description", String.format("在%d天内，有%.0f%%的可能性最大亏损不超过%.2f元",
                        holdingPeriodDays, confidenceLevel * 100, var)
        );
    }

    /**
     * 计算夏普比率。
     *
     * @param portfolioReturn 组合预期收益率
     * @param riskFreeRate    无风险利率（如国债收益率）
     * @param portfolioVolatility 组合波动率
     * @return 夏普比率
     */
    @Tool(description = "计算夏普比率，衡量风险调整后的收益")
    public Map<String, Object> calculateSharpeRatio(
            @ToolParam(description = "组合预期收益率，如 0.12 表示12%") double portfolioReturn,
            @ToolParam(description = "无风险利率，如 0.03 表示3%") double riskFreeRate,
            @ToolParam(description = "组合波动率，如 0.20 表示20%") double portfolioVolatility
    ) {
        double sharpeRatio = (portfolioReturn - riskFreeRate) / portfolioVolatility;

        String evaluation;
        if (sharpeRatio > 1.0) {
            evaluation = "优秀";
        } else if (sharpeRatio > 0.5) {
            evaluation = "良好";
        } else if (sharpeRatio > 0) {
            evaluation = "一般";
        } else {
            evaluation = "较差";
        }

        return Map.of(
                "portfolioReturn", portfolioReturn * 100 + "%",
                "riskFreeRate", riskFreeRate * 100 + "%",
                "portfolioVolatility", portfolioVolatility * 100 + "%",
                "sharpeRatio", String.format("%.2f", sharpeRatio),
                "evaluation", evaluation,
                "description", "夏普比率" + evaluation + "，每承担一单位风险可获得" +
                        String.format("%.2f", sharpeRatio) + "单位超额收益"
        );
    }

    private record RiskAssessment(String level, String description) {}

    private RiskAssessment assessRisk(int stockAllocation) {
        if (stockAllocation >= 80) return new RiskAssessment("高风险", "高风险组合，适合风险承受能力强、追求高收益的投资者");
        if (stockAllocation >= 60) return new RiskAssessment("中高风险", "中高风险组合，适合有一定风险承受能力、追求稳健收益的投资者");
        if (stockAllocation >= 40) return new RiskAssessment("中风险", "中风险组合，适合平衡型投资者，追求收益与风险的平衡");
        if (stockAllocation >= 20) return new RiskAssessment("中低风险", "中低风险组合，适合保守型投资者，注重本金安全");
        return new RiskAssessment("低风险", "低风险组合，适合非常保守的投资者，以保值为主");
    }
}
