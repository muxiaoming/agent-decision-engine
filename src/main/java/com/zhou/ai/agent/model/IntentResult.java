package com.zhou.ai.agent.model;

/**
 * 意图分类Agent的结构化输出实体。
 *
 * <p>供Spring AI的 {@code .entity(IntentResult.class)} 使用，
 * 强制LLM输出合法JSON映射到本record，彻底消除自由文本解析的脆弱性。
 *
 * <p><b>字段说明：</b>
 * <ul>
 *   <li>{@code isInvestment} — 是否为投资相关意图（true/false）</li>
 *   <li>{@code desc} — 分类理由简述（用于日志和调试）</li>
 * </ul>
 *
 * @since 2026-06-30
 */
public record IntentResult(
        /** 是否为金融投资相关需求 */
        Boolean isInvestment,

        /** 分类理由描述 */
        String desc
) {
    public boolean isNonInvestment() {
        return isInvestment == null || !isInvestment;
    }
}
