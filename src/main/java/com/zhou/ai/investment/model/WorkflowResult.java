package com.zhou.ai.investment.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 工作流结果 DTO。
 * 包装整个投资决策工作流的执行结果。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkflowResult(
        /**
         * 响应状态。
         * 可选值: "success", "partial", "failed"
         */
        String status,

        /**
         * 对话线程ID。
         */
        String threadId,

        /**
         * 工作流数据，包含各模块的启用状态和结果。
         */
        WorkflowData data,

        /**
         * 决策步骤列表。
         */
        List<WorkflowStep> steps,

        /**
         * 最终投资建议。
         */
        String finalAdvice,

        /**
         * 风险提示。
         */
        String riskWarning,

        /**
         * 执行耗时（毫秒）。
         */
        long durationMs,

        /**
         * Token用量信息。
         */
        TokenUsage tokenUsage,

        /**
         * 使用的模型名称。
         */
        String model,

        /**
         * 错误信息（如果整体失败）。
         */
        String error
) {
    /**
     * 创建成功的响应。
     */
    public static WorkflowResult success(String threadId, WorkflowData data,
                                         List<WorkflowStep> steps, String finalAdvice,
                                         String riskWarning, long durationMs,
                                         TokenUsage tokenUsage, String model) {
        return new WorkflowResult("success", threadId, data, steps, finalAdvice,
                riskWarning, durationMs, tokenUsage, model, null);
    }

    /**
     * 创建部分成功的响应（某些步骤失败或跳过）。
     */
    public static WorkflowResult partial(String threadId, WorkflowData data,
                                         List<WorkflowStep> steps, String finalAdvice,
                                         String riskWarning, long durationMs,
                                         TokenUsage tokenUsage, String model) {
        return new WorkflowResult("partial", threadId, data, steps, finalAdvice,
                riskWarning, durationMs, tokenUsage, model, null);
    }

    /**
     * 创建失败的响应。
     */
    public static WorkflowResult failed(String error) {
        return new WorkflowResult("failed", null, null, null, null,
                null, 0, null, null, error);
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
        return steps.stream().filter(WorkflowStep::isSuccessful).count();
    }

    /**
     * 获取失败的步骤数。
     */
    public long getFailedStepCount() {
        if (steps == null) return 0;
        return steps.stream().filter(WorkflowStep::isFailed).count();
    }

    /**
     * 获取跳过的步骤数。
     */
    public long getSkippedStepCount() {
        if (steps == null) return 0;
        return steps.stream().filter(WorkflowStep::isSkipped).count();
    }

    /**
     * 获取总步骤数。
     */
    public int getTotalStepCount() {
        if (steps == null) return 0;
        return steps.size();
    }
}
