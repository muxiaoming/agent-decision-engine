package com.zhou.ai.investment.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 投资决策流式事件 DTO。
 * 用于流式返回决策步骤。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InvestmentStepEvent(
        /**
         * 事件类型。
         * - "step_start": 步骤开始
         * - "step_complete": 步骤完成
         * - "step_error": 步骤失败
         * - "decision_complete": 决策完成
         * - "error": 错误
         */
        String type,

        /**
         * 步骤编号（从1开始）。
         */
        Integer step,

        /**
         * 步骤名称。
         */
        String name,

        /**
         * 使用的技能。
         */
        String skill,

        /**
         * 步骤状态。
         */
        String status,

        /**
         * 步骤结果。
         */
        String result,

        /**
         * 错误信息。
         */
        String error,

        /**
         * 最终投资建议（仅在 decision_complete 事件中）。
         */
        String finalAdvice,

        /**
         * 风险提示（仅在 decision_complete 事件中）。
         */
        String riskWarning,

        /**
         * 线程ID。
         */
        String threadId,

        /**
         * 执行耗时（毫秒）。
         */
        Long durationMs
) {
    /**
     * 创建步骤开始事件。
     */
    public static InvestmentStepEvent stepStart(int step, String name, String skill) {
        return new InvestmentStepEvent("step_start", step, name, skill, "running", null, null, null, null, null, null);
    }

    /**
     * 创建步骤完成事件。
     */
    public static InvestmentStepEvent stepComplete(int step, String name, String skill, String result) {
        return new InvestmentStepEvent("step_complete", step, name, skill, "completed", result, null, null, null, null, null);
    }

    /**
     * 创建步骤失败事件。
     */
    public static InvestmentStepEvent stepError(int step, String name, String skill, String error) {
        return new InvestmentStepEvent("step_error", step, name, skill, "failed", null, error, null, null, null, null);
    }

    /**
     * 创建决策完成事件。
     */
    public static InvestmentStepEvent decisionComplete(
            String threadId,
            String finalAdvice,
            String riskWarning,
            long durationMs) {
        return new InvestmentStepEvent("decision_complete", null, null, null, "completed",
                null, null, finalAdvice, riskWarning, threadId, durationMs);
    }

    /**
     * 创建错误事件。
     */
    public static InvestmentStepEvent error(String error) {
        return new InvestmentStepEvent("error", null, null, null, "failed", null, error, null, null, null, null);
    }
}
