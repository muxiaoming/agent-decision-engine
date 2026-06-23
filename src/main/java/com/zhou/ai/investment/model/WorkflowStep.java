package com.zhou.ai.investment.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 工作流步骤 DTO。
 * 记录每个决策步骤的详细执行信息。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkflowStep(
        /**
         * 步骤编号。
         */
        int step,

        /**
         * 步骤名称。
         */
        String name,

        /**
         * 执行模块。
         * 可选值: "skills", "rag", "tools", "graph"
         */
        String module,

        /**
         * 步骤状态。
         * 可选值: "completed", "failed", "skipped"
         */
        String status,

        /**
         * 步骤结果。
         */
        String result,

        /**
         * 错误信息（如果步骤失败）。
         */
        String error,

        /**
         * RAG检索到的来源（仅RAG步骤）。
         */
        List<String> ragSources,

        /**
         * 工具调用记录（仅Tools步骤）。
         */
        List<ToolCall> toolCalls,

        /**
         * 执行的图节点（仅Graph步骤）。
         */
        List<String> graphNodes,

        /**
         * 执行耗时（毫秒）。
         */
        long durationMs,

        /**
         * Token用量信息。
         */
        TokenUsage tokenUsage
) {
    /**
     * 创建成功的步骤记录（简化版）。
     */
    public static WorkflowStep success(int step, String name, String module,
                                       String result, long durationMs) {
        return new WorkflowStep(step, name, module, "completed", result, null,
                null, null, null, durationMs, null);
    }

    /**
     * 创建成功的步骤记录（完整版）。
     */
    public static WorkflowStep successWithData(int step, String name, String module,
                                               String result, List<String> ragSources,
                                               List<ToolCall> toolCalls, List<String> graphNodes,
                                               long durationMs, TokenUsage tokenUsage) {
        return new WorkflowStep(step, name, module, "completed", result, null,
                ragSources, toolCalls, graphNodes, durationMs, tokenUsage);
    }

    /**
     * 创建失败的步骤记录。
     */
    public static WorkflowStep failed(int step, String name, String module,
                                      String error, long durationMs) {
        return new WorkflowStep(step, name, module, "failed", null, error,
                null, null, null, durationMs, null);
    }

    /**
     * 创建跳过的步骤记录。
     */
    public static WorkflowStep skipped(int step, String name, String module,
                                       String reason, long durationMs) {
        return new WorkflowStep(step, name, module, "skipped", null, reason,
                null, null, null, durationMs, null);
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

    /**
     * 判断步骤是否被跳过。
     */
    public boolean isSkipped() {
        return "skipped".equals(status);
    }
}
