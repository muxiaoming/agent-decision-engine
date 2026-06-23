package com.zhou.ai.investment.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 工作流数据 DTO。
 * 跟踪各模块的启用状态和执行结果。
 */
public record WorkflowData(
        /**
         * 是否启用RAG知识检索。
         */
        boolean enableRAG,

        /**
         * 是否启用工具调用。
         */
        boolean enableTools,

        /**
         * 是否启用Graph工作流。
         */
        boolean enableGraph,

        /**
         * RAG检索结果。
         */
        Map<String, Object> ragResults,

        /**
         * 工具调用结果。
         */
        Map<String, Object> toolResults,

        /**
         * Graph工作流结果。
         */
        Map<String, Object> graphResults
) {
    /**
     * 创建默认工作流数据（所有模块启用）。
     */
    public static WorkflowData withDefaults() {
        return new WorkflowData(true, true, true,
                new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    /**
     * 创建指定配置的工作流数据。
     */
    public static WorkflowData of(boolean enableRAG, boolean enableTools, boolean enableGraph) {
        return new WorkflowData(enableRAG, enableTools, enableGraph,
                new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    /**
     * 添加RAG检索结果。
     */
    public WorkflowData addRAGResult(String key, Object value) {
        ragResults.put(key, value);
        return this;
    }

    /**
     * 添加工具调用结果。
     */
    public WorkflowData addToolResult(String key, Object value) {
        toolResults.put(key, value);
        return this;
    }

    /**
     * 添加Graph工作流结果。
     */
    public WorkflowData addGraphResult(String key, Object value) {
        graphResults.put(key, value);
        return this;
    }

    /**
     * 检查RAG是否启用。
     */
    public boolean isRAGEnabled() {
        return enableRAG;
    }

    /**
     * 检查工具是否启用。
     */
    public boolean isToolsEnabled() {
        return enableTools;
    }

    /**
     * 检查Graph是否启用。
     */
    public boolean isGraphEnabled() {
        return enableGraph;
    }
}
