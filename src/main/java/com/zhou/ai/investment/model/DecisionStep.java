package com.zhou.ai.investment.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 投资决策步骤 DTO。
 * 表示决策流程中的一个步骤。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DecisionStep(
        /**
         * 步骤编号（从1开始）。
         */
        int step,

        /**
         * 步骤名称。
         * 示例: "市场分析", "投资推荐", "风险评估" 等
         */
        String name,

        /**
         * 使用的技能名称。
         * 示例: "market-analysis", "investment-recommendation" 等
         * 如果是综合分析，可能为 "综合分析" 或 null
         */
        String skill,

        /**
         * 步骤状态。
         * 可选值: "completed", "failed", "skipped"
         */
        String status,

        /**
         * 步骤结果。
         * 包含该步骤的详细输出。
         */
        String result,

        /**
         * 错误信息（如果步骤失败）。
         */
        String error
) {
    /**
     * 创建成功的步骤。
     */
    public static DecisionStep success(int step, String name, String skill, String result) {
        return new DecisionStep(step, name, skill, "completed", result, null);
    }

    /**
     * 创建失败的步骤。
     */
    public static DecisionStep failed(int step, String name, String skill, String error) {
        return new DecisionStep(step, name, skill, "failed", null, error);
    }

    /**
     * 创建跳过的步骤。
     */
    public static DecisionStep skipped(int step, String name, String skill, String reason) {
        return new DecisionStep(step, name, skill, "skipped", null, reason);
    }

    /**
     * 判断步骤是否成功。
     */
    public boolean isSuccessful() {
        return "completed".equals(status);
    }

    /**
     * 判断步骤是否失败。
     */
    public boolean isFailed() {
        return "failed".equals(status);
    }
}
