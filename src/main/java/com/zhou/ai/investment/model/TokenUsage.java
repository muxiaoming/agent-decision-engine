package com.zhou.ai.investment.model;

/**
 * Token 用量 DTO。
 * 记录投资决策过程中的 Token 消耗。
 */
public record TokenUsage(
        /**
         * 提示词 Token 数。
         */
        int promptTokens,

        /**
         * 补全 Token 数。
         */
        int completionTokens,

        /**
         * 总 Token 数。
         */
        int totalTokens
) {
    /**
     * 创建 Token 用量。
     */
    public static TokenUsage of(int promptTokens, int completionTokens) {
        return new TokenUsage(promptTokens, completionTokens, promptTokens + completionTokens);
    }

    /**
     * 累加另一个 Token 用量。
     */
    public TokenUsage add(TokenUsage other) {
        if (other == null) return this;
        return new TokenUsage(
                this.promptTokens + other.promptTokens,
                this.completionTokens + other.completionTokens,
                this.totalTokens + other.totalTokens
        );
    }
}
