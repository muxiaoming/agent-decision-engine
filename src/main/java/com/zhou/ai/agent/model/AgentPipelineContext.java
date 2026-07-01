package com.zhou.ai.agent.model;

import com.alibaba.cloud.ai.graph.OverAllState;

/**
 * 多Agent管线上下文 — 统一管理状态读取、空值兜底、Prompt组装。
 *
 * <p>每个Agent的 asNodeAction() 先通过 {@link #from(OverAllState)} 获取上下文，
 * 再使用 {@code String.format(FMT, ctx.field1(), ctx.field2(), ...)} 构建 prompt。
 *
 * <p>空值兜底：所有 getter 在值为 null/blank 时返回 {@code "（上游未提供数据）"}，
 * 下游 Agent 无需重复判断。
 */
public record AgentPipelineContext(
        String userMessage,
        String perceptionResult,
        String retrievalResult,
        String dataResult,
        String reasoningResult,
        String decisionResult,
        String modelName
) {

    private static final String NOT_PROVIDED = "（上游未提供数据）";
    private static final String DEFAULT_MODEL = "openAiChatModel";

    /**
     * 从 OverAllState 提取全部状态值，空值自动兜底。
     */
    public static AgentPipelineContext from(OverAllState state) {
        return new AgentPipelineContext(
                nf(state, AgentGraphState.USER_MESSAGE),
                nf(state, AgentGraphState.PERCEPTION_RESULT),
                nf(state, AgentGraphState.RETRIEVAL_RESULT),
                nf(state, AgentGraphState.DATA_RESULT),
                nf(state, AgentGraphState.REASONING_RESULT),
                nf(state, AgentGraphState.DECISION_RESULT),
                model(state, AgentGraphState.MODEL_NAME)
        );
    }

    /** 仅包含上游结果 + modelName（用于决策生成、流程编排等不需要 userMessage 的场景） */
    public static AgentPipelineContext resultsOnly(OverAllState state) {
        return new AgentPipelineContext(
                NOT_PROVIDED,
                nf(state, AgentGraphState.PERCEPTION_RESULT),
                nf(state, AgentGraphState.RETRIEVAL_RESULT),
                nf(state, AgentGraphState.DATA_RESULT),
                nf(state, AgentGraphState.REASONING_RESULT),
                nf(state, AgentGraphState.DECISION_RESULT),
                model(state, AgentGraphState.MODEL_NAME)
        );
    }

    // ==================== 辅助方法 ====================

    private static String nf(OverAllState state, String key) {
        String val = (String) state.value(key).orElse(null);
        return (val == null || val.isBlank()) ? NOT_PROVIDED : val;
    }

    private static String model(OverAllState state, String key) {
        String val = (String) state.value(key).orElse(null);
        return (val == null || val.isBlank()) ? DEFAULT_MODEL : val;
    }
}