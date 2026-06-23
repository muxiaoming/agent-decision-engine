package com.zhou.ai.investment.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 投资决策请求 DTO。
 * 接收用户的投资需求和配置参数。
 */
public record InvestmentDecisionRequest(
        /**
         * 用户投资需求描述。
         * 示例: "我想投资科技股，预算10万，风险承受能力中等"
         */
        String message,

        /**
         * 使用的模型名称。
         * 可选值: deepSeekChatModel, openAiChatModel, dashscopeChatModel
         * 默认值: deepSeekChatModel
         */
        @JsonProperty(defaultValue = "deepSeekChatModel")
        String modelName,

        /**
         * 对话线程ID。
         * 用于多轮对话，保持上下文。
         * 如果不提供，系统自动生成。
         */
        String threadId,

        /**
         * 是否启用RAG知识检索。
         * 默认值: true
         */
        @JsonProperty(defaultValue = "true")
        Boolean enableRAG,

        /**
         * 是否启用工具调用。
         * 默认值: true
         */
        @JsonProperty(defaultValue = "true")
        Boolean enableTools,

        /**
         * 是否启用Graph工作流。
         * 默认值: true
         */
        @JsonProperty(defaultValue = "true")
        Boolean enableGraph
) {
    /**
     * 创建默认请求。
     */
    public static InvestmentDecisionRequest of(String message) {
        return new InvestmentDecisionRequest(message, "deepSeekChatModel", null, null, null, null);
    }

    /**
     * 创建带模型的请求。
     */
    public static InvestmentDecisionRequest of(String message, String modelName) {
        return new InvestmentDecisionRequest(message, modelName, null, null, null, null);
    }

    /**
     * 创建带线程ID的请求。
     */
    public static InvestmentDecisionRequest of(String message, String modelName, String threadId) {
        return new InvestmentDecisionRequest(message, modelName, threadId, null, null, null);
    }

    /**
     * 创建完整配置的请求。
     */
    public static InvestmentDecisionRequest of(String message, String modelName, String threadId,
                                               Boolean enableRAG, Boolean enableTools, Boolean enableGraph) {
        return new InvestmentDecisionRequest(message, modelName, threadId, enableRAG, enableTools, enableGraph);
    }

    /**
     * 获取有效的模型名称。
     */
    public String effectiveModelName() {
        return modelName != null ? modelName : "deepSeekChatModel";
    }

    /**
     * 获取有效的线程ID。
     */
    public String effectiveThreadId() {
        return threadId != null ? threadId : "investment-" + System.currentTimeMillis();
    }

    /**
     * 获取有效的RAG启用状态。
     */
    public boolean effectiveEnableRAG() {
        return enableRAG != null ? enableRAG : true;
    }

    /**
     * 获取有效的Tools启用状态。
     */
    public boolean effectiveEnableTools() {
        return enableTools != null ? enableTools : true;
    }

    /**
     * 获取有效的Graph启用状态。
     */
    public boolean effectiveEnableGraph() {
        return enableGraph != null ? enableGraph : true;
    }
}
