package com.example.ai.graph.service;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class GraphWorkflowService {

    private final CompiledGraph compiledGraph;

    public GraphWorkflowService(StateGraph workflowGraph) throws GraphStateException {
        this.compiledGraph = workflowGraph.compile();
    }

    /**
     * 执行图工作流。
     */
    public Map<String, Object> execute(String input) {
        try {
            // 适配 spring-ai-alibaba 1.1.2+ API: call → invoke
            java.util.Optional<OverAllState> result = compiledGraph.invoke(Map.of("input", input), RunnableConfig.builder().build());
            OverAllState state = result.get();

            return Map.of(
                    "output", state.value("output").orElse("无输出"),
                    "category", state.value("category").orElse("unknown")
            );
        } catch (Exception e) {
            throw new RuntimeException("图工作流执行失败: " + e.getMessage(), e);
        }
    }
}
