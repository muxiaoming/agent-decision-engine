package com.zhou.ai.investment.model;

/**
 * LLM 意图分类的结构化输出模型。
 * Spring AI 的 .entity() 方法会强制 LLM 输出合法 JSON 到这个 record，
 * 确保 isInvestment 字段始终是 true/false，不会出现 "YES." / "No, but..." 等不可解析的值。
 */
public record IntentClassification(
        /** 是否与金融投资相关 */
        boolean isInvestment,

        /** 意图类型细分：investment / greeting / weather / chitchat / other */
        String intentType,

        /** 简要理由（用于日志和调试） */
        String reason
) {
    public boolean isNonInvestment() {
        return !isInvestment;
    }
}
