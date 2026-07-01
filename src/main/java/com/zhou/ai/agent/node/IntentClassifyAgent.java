package com.zhou.ai.agent.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.zhou.ai.agent.model.AgentGraphState;
import com.zhou.ai.agent.model.IntentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 意图分类Agent（第1步：前置意图分类）。
 *
 * <p>两层递进意图分类器：
 * <ul>
 *   <li><b>Tier 1 — 投资关键词白名单</b>：命中关键词（股票/基金/持仓等）直接放行，
 *       这是一个「正向加速」，不命中≠非投资。</li>
 *   <li><b>Tier 2 — LLM .entity() 结构化分类</b>：未命中白名单时，使用 Spring AI
 *       {@code .entity(IntentResult.class)} 强制 LLM 输出合法 JSON 到 {@link IntentResult} record，
 *       依靠布尔字段判断分类，彻底消除字符串 contains 模糊匹配。</li>
 * </ul>
 *
 * <p><b>降级策略：</b>LLM异常、实体解析失败、返回空均兜底判定为投资需求（isInvestment=true）。
 *
 * <p><b>与原有{@code IntentClassifier}对比：</b>
 * 完全相同策略内聚在Agent中，由Graph条件路由驱动后续流程。
 *
 * @since 2026-06-30
 */
@Component
public class IntentClassifyAgent {

    private static final Logger log = LoggerFactory.getLogger(IntentClassifyAgent.class);

    /**
     * Tier 1: 投资关键词白名单（仅做正向加速，不命中≠非投资）。
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

    private final ChatClient chatClient;

    public IntentClassifyAgent(@Qualifier("intentClassifyChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 将本Agent封装为Graph节点可执行的AsyncNodeAction。
     */
    public AsyncNodeAction asNodeAction() {
        return state -> {
            String userMessage = (String) state.value(AgentGraphState.USER_MESSAGE).orElse("");
            log.info("[意图分类Agent] 分类用户输入: {}", truncate(userMessage));

            // ── Tier 1: 投资关键词白名单（正向加速） ──
            if (matchesKeyword(userMessage)) {
                log.debug("[意图分类Agent] Tier 1 whitelist hit");
                return CompletableFuture.completedFuture(Map.of(
                        AgentGraphState.INTENT_RESULT, "关键词白名单命中，判定为投资需求",
                        AgentGraphState.IS_INVESTMENT, "true"
                ));
            }

            // ── Tier 2: LLM .entity() 结构化分类 ──
            return classifyByEntity(userMessage);
        };
    }

    /**
     * Tier 2: Spring AI .entity() 结构化分类。
     * LLM 必须输出合法 JSON 映射到 IntentResult record。
     */
    private CompletableFuture<Map<String, Object>> classifyByEntity(String userMessage) {
        try {
            String systemPrompt = """
                    你是严格的问题分类器。判断用户输入是否与金融投资相关。

                    与金融投资相关的话题包括（但不限于）：
                    - 股票、基金、债券、ETF、指数
                    - 投资理财、资产配置、风险管理
                    - 市场分析、行业研究、财报分析
                    - 仓位管理、定投、交易策略
                    - 收益率、夏普比率、估值

                    与金融投资无关的话题包括：
                    - 问候寒暄（你好、谢谢、再见）
                    - 天气、新闻、娱乐、体育
                    - 编程、技术问题
                    - 日常聊天、情绪表达

                    你**必须**以 JSON 格式输出，不要输出任何其他内容。
                    输出格式（严格 JSON）：
                    {"isInvestment": true/false, "desc": "简短理由"}
                    """;

            String userPrompt = "请分类以下用户输入：\n%s".formatted(userMessage);

            IntentResult result = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .entity(IntentResult.class);

            boolean isInvestment = result.isInvestment() != null && result.isInvestment();
            String desc = result.desc() != null ? result.desc() : "无理由";

            log.info("[意图分类Agent] entity分类: isInvestment={}, reason='{}'", isInvestment, desc);

            return CompletableFuture.completedFuture(Map.of(
                    AgentGraphState.INTENT_RESULT, desc,
                    AgentGraphState.IS_INVESTMENT, String.valueOf(isInvestment)
            ));

        } catch (Exception e) {
            log.warn("[意图分类Agent] entity异常，默认放行: {}", e.getMessage());
            return CompletableFuture.completedFuture(Map.of(
                    AgentGraphState.INTENT_RESULT, "LLM分类异常，默认放行: " + e.getMessage(),
                    AgentGraphState.IS_INVESTMENT, "true"
            ));
        }
    }

    private boolean matchesKeyword(String message) {
        String normalized = (message != null) ? message.toLowerCase().trim() : "";
        return INVESTMENT_WHITELIST.stream().anyMatch(kw -> normalized.contains(kw.toLowerCase()));
    }

    private static String truncate(String text) {
        return text != null && text.length() > 80 ? text.substring(0, 80) + "..." : text;
    }
}
