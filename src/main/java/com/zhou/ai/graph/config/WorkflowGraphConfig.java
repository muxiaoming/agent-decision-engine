package com.zhou.ai.graph.config;

import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

/**
 * Graph 工作流配置：定义投资决策的简化工作流，包含投资分析与结果输出。
 */
@Configuration
public class WorkflowGraphConfig {

    @Bean
    public StateGraph workflowGraph() throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = () -> {
            Map<String, com.alibaba.cloud.ai.graph.KeyStrategy> strategies = new java.util.HashMap<>();
            strategies.put("input", new ReplaceStrategy());
            strategies.put("processedResult", new ReplaceStrategy());
            strategies.put("output", new ReplaceStrategy());
            return strategies;
        };

        return new StateGraph(keyStrategyFactory)
                .addNode("process_investment", processInvestmentNode())
                .addNode("output", outputNode())
                .addEdge(START, "process_investment")
                .addEdge("process_investment", "output")
                .addEdge("output", END);
    }

    private AsyncNodeAction processInvestmentNode() {
        return state -> {
            String input = (String) state.value("input").orElse("");
            String result = "【投资分析】" + input + " —— 已基于市场趋势、风险评估和资产配置角度进行分析。";
            return CompletableFuture.completedFuture(Map.of("processedResult", result));
        };
    }

    private AsyncNodeAction outputNode() {
        return state -> {
            String processed = (String) state.value("processedResult").orElse("");
            String output = "分析结果:\n" + processed;
            return CompletableFuture.completedFuture(Map.of("output", output));
        };
    }
}
