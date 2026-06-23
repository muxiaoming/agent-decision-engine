package com.zhou.ai.investment.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 投资决策响应 DTO。
 * 包含完整的投资决策结果。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InvestmentDecisionResponse(
        /**
         * 响应状态。
         * 可选值: "success", "partial", "failed"
         */
        String status,

        /**
         * 对话线程ID。
         * 用于后续多轮对话。
         */
        String threadId,

        /**
         * 决策步骤列表。
         * 包含所有执行步骤的详细结果。
         */
        List<DecisionStep> steps,

        /**
         * 最终投资建议。
         * 综合所有步骤生成的完整建议。
         */
        String finalAdvice,

        /**
         * 风险提示。
         * 投资风险声明。
         */
        String riskWarning,

        /**
         * 执行耗时（毫秒）。
         */
        long durationMs,

        /**
         * Token 用量信息。
         */
        TokenUsage tokenUsage,

        /**
         * 使用的模型名称。
         */
        String model,

        /**
         * 错误信息（如果整体失败）。
         */
        String error,

        /**
         * 工作流结果（新版本）。
         * 包含更详细的模块数据和步骤信息。
         */
        WorkflowResult workflow
) {
    /**
     * 创建成功的响应。
     */
    public static InvestmentDecisionResponse success(
            String threadId,
            List<DecisionStep> steps,
            String finalAdvice,
            String riskWarning,
            long durationMs,
            TokenUsage tokenUsage,
            String model) {
        return new InvestmentDecisionResponse(
                "success",
                threadId,
                steps,
                finalAdvice,
                riskWarning,
                durationMs,
                tokenUsage,
                model,
                null,
                null
        );
    }

    /**
     * 创建带工作流结果的成功响应。
     */
    public static InvestmentDecisionResponse successWithWorkflow(
            String threadId,
            List<DecisionStep> steps,
            String finalAdvice,
            String riskWarning,
            long durationMs,
            TokenUsage tokenUsage,
            String model,
            WorkflowResult workflow) {
        return new InvestmentDecisionResponse(
                "success",
                threadId,
                steps,
                finalAdvice,
                riskWarning,
                durationMs,
                tokenUsage,
                model,
                null,
                workflow
        );
    }

    /**
     * 创建部分成功的响应（某些步骤失败）。
     */
    public static InvestmentDecisionResponse partial(
            String threadId,
            List<DecisionStep> steps,
            String finalAdvice,
            String riskWarning,
            long durationMs,
            TokenUsage tokenUsage,
            String model) {
        return new InvestmentDecisionResponse(
                "partial",
                threadId,
                steps,
                finalAdvice,
                riskWarning,
                durationMs,
                tokenUsage,
                model,
                null,
                null
        );
    }

    /**
     * 创建失败的响应。
     */
    public static InvestmentDecisionResponse failed(String error) {
        return new InvestmentDecisionResponse(
                "failed",
                null,
                null,
                null,
                null,
                0,
                null,
                null,
                error,
                null
        );
    }

    /**
     * 判断是否完全成功。
     */
    public boolean isFullySuccess() {
        return "success".equals(status);
    }

    /**
     * 判断是否部分成功。
     */
    public boolean isPartiallySuccess() {
        return "partial".equals(status);
    }

    /**
     * 判断是否失败。
     */
    public boolean isFailed() {
        return "failed".equals(status);
    }

    /**
     * 获取成功的步骤数。
     */
    public long getSuccessfulStepCount() {
        if (steps == null) return 0;
        return steps.stream().filter(DecisionStep::isSuccessful).count();
    }

    /**
     * 获取失败的步骤数。
     */
    public long getFailedStepCount() {
        if (steps == null) return 0;
        return steps.stream().filter(DecisionStep::isFailed).count();
    }
}
