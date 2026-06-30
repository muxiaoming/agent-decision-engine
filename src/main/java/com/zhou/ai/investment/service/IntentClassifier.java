package com.zhou.ai.investment.service;

import com.zhou.ai.common.router.ModelRouter;
import com.zhou.ai.investment.model.IntentClassification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 两层递进意图分类器。
 *
 * <p><b>设计思路：</b>
 * <ul>
 *   <li><b>Tier 1 - 投资关键词白名单</b>：命中关键词（股票/基金/持仓等）直接放行，
 *       这是一个「正向加速」，≠「阻断」。不命中不代表非投资。</li>
 *   <li><b>Tier 2 - LLM 结构化分类</b>：未命中白名单时，用 Spring AI .entity()
 *       强制 LLM 输出合法 JSON，绝不产生不可解析的自由文本。</li>
 * </ul>
 *
 * <p><b>核心原则：</b>
 * <ul>
 *   <li>不用「非投资关键词黑名单」——"你好，请帮我分析持仓"会被误杀</li>
 *   <li>LLM 异常时默认放行（保守兜底），宁可多跑几步也不误杀真实需求</li>
 * </ul>
 */
@Component
public class IntentClassifier {

    private static final Logger log = LoggerFactory.getLogger(IntentClassifier.class);

    /**
     * Tier 1: 投资关键词白名单（仅做正向加速，不命中 ≠ 非投资）。
     * 当用户消息包含以下任一关键词时，快速放行，跳过 LLM 分类。
     */
    private static final List<String> INVESTMENT_WHITELIST = List.of(
            "股票", "基金", "债券", "投资", "理财", "portfolio",
            "市场", "a股", "上证", "深证", "股市", "大盘",
            "风险", "收益", "回报率", "夏普", "var", "收益率",
            "资产配置", "仓位", "建仓", "加仓", "减仓", "持仓",
            "分红", "股息", "财报", "估值", "市盈率", "市净率",
            "k线", "技术面", "基本面", "趋势",
            "买入", "卖出", "持有", "定投", "申购", "赎回",
            "科技股", "消费股", "金融股", "板块", "行业",
            "etf", "指数", "纳斯达克", "标普", "道琼斯",
            "对冲", "套利", "杠杆", "做多", "做空",
            "投资组合", "分散", "配置",
            "盈利", "亏损", "涨", "跌", "涨幅", "跌幅",
            "开户", "交易", "手续费", "佣金"
    );

    private final ModelRouter modelRouter;

    public IntentClassifier(ModelRouter modelRouter) {
        this.modelRouter = modelRouter;
    }

    /**
     * 两层递进分类。
     *
     * @return true = 投资类消息，继续全流程
     *         false = 非投资类消息，走对话回复
     */
    public boolean isInvestmentRelated(String message, String modelName) {
        String normalized = message.toLowerCase().trim();

        // ── Tier 1: 投资关键词白名单（正向加速） ──
        // 命中白名单 → 直接放行，0 次 LLM 调用
        // 不命中白名单 → 不意味着非投资，交给 Tier 2 判断
        if (matchesAny(normalized, INVESTMENT_WHITELIST)) {
            log.debug("Tier 1 whitelist hit: message='{}'", message);
            return true;
        }

        // ── Tier 2: LLM 结构化分类 ──
        // 仅「模糊情况」才调用 LLM，大幅节省成本
        return classifyByLLM(message, modelName);
    }

    /**
     * Tier 2: Spring AI .entity() 结构化分类。
     * 强制 LLM 输出合法 JSON 到 IntentClassification record，
     * 彻底消除自由文本解析的脆弱性。
     */
    private boolean classifyByLLM(String message, String modelName) {
        try {
            String systemPrompt = """
                    你是严格的问题分类器。判断用户输入是否与金融投资相关。

                    与金融投资相关的话题包括（但不限于）：
                    - 股票、基金、债券、ETF、指数
                    - 投资理财、资产配置、风险管理
                    - 市场分析、行业研究、财报分析
                    - 仓位管理、定投、交易策略
                    - 收益率、夏普比率、估值
                    - 任何涉及钱、账户、收益的问题

                    与金融投资无关的话题包括：
                    - 问候寒暄（你好、谢谢、再见）
                    - 天气、新闻、娱乐、体育
                    - 编程、技术问题
                    - 日常聊天、情绪表达

                    你**必须**以 JSON 格式输出，不要输出任何其他内容。
                    输出格式：
                    {
                      "isInvestment": true/false,
                      "intentType": "investment/greeting/weather/chitchat/other",
                      "reason": "简短理由"
                    }
                    """;

            String userPrompt = "请分类以下用户输入：\n" + message;

            IntentClassification result = modelRouter.route(modelName).prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .entity(IntentClassification.class);

            log.debug("Tier 2 classification: isInvestment={}, type={}, reason='{}'",
                    result.isInvestment(), result.intentType(), result.reason());

            return result.isInvestment();

        } catch (Exception e) {
            // ── Tier 2 降级：LLM 异常时默认放行 ──
            // 宁可多跑几步完整的投资流程，也不要误杀真实需求
            log.warn("Tier 2 classification failed, default to investment (conservative): {}", e.getMessage());
            return true;
        }
    }

    private boolean matchesAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }
}
