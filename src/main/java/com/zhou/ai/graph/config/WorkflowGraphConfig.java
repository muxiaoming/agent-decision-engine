package com.zhou.ai.graph.config;

import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
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
 * Graph 工作流配置：定义分类→处理→输出的多步骤工作流。
 * 包含条件分支路由。
 */
@Configuration
public class WorkflowGraphConfig {

    @Bean
    public StateGraph workflowGraph() throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = () -> {
            Map<String, com.alibaba.cloud.ai.graph.KeyStrategy> strategies = new java.util.HashMap<>();
            strategies.put("input", new ReplaceStrategy());
            strategies.put("category", new ReplaceStrategy());
            strategies.put("processedResult", new ReplaceStrategy());
            strategies.put("output", new ReplaceStrategy());
            return strategies;
        };

        return new StateGraph(keyStrategyFactory)
                .addNode("classify", classifyNode())
                .addNode("process_tech", processTechNode())
                .addNode("process_lifestyle", processLifestyleNode())
                .addNode("process_general", processGeneralNode())
                .addNode("output", outputNode())
                .addEdge(START, "classify")
                .addConditionalEdges("classify", categoryRouter(),
                        Map.of("technical", "process_tech",
                                "lifestyle", "process_lifestyle",
                                "general", "process_general"))
                .addEdge("process_tech", "output")
                .addEdge("process_lifestyle", "output")
                .addEdge("process_general", "output")
                .addEdge("output", END);
    }

    private AsyncNodeAction classifyNode() {
        return state -> {
            String input = (String) state.value("input").orElse("");
            String category = classifyContent(input);
            return CompletableFuture.completedFuture(Map.of("category", category));
        };
    }

    private AsyncEdgeAction categoryRouter() {
        return state -> CompletableFuture.completedFuture(
                (String) state.value("category").orElse("general"));
    }

    private AsyncNodeAction processTechNode() {
        return state -> {
            String input = (String) state.value("input").orElse("");
            String result = "【技术分析】" + input + " —— 这段内容涉及技术领域，建议从技术实现和工程实践角度分析。";
            return CompletableFuture.completedFuture(Map.of("processedResult", result));
        };
    }

    private AsyncNodeAction processLifestyleNode() {
        return state -> {
            String input = (String) state.value("input").orElse("");
            String result = "【生活休闲】" + input + " —— 这段内容与日常生活相关，建议从用户体验和生活品质角度探讨。";
            return CompletableFuture.completedFuture(Map.of("processedResult", result));
        };
    }

    private AsyncNodeAction processGeneralNode() {
        return state -> {
            String input = (String) state.value("input").orElse("");
            String result = "【通用分析】" + input + " —— 这段内容属于一般性描述，建议从综合角度进行分析。";
            return CompletableFuture.completedFuture(Map.of("processedResult", result));
        };
    }

    private AsyncNodeAction outputNode() {
        return state -> {
            String processed = (String) state.value("processedResult").orElse("");
            String category = (String) state.value("category").orElse("general");
            String output = "分类: " + category + "\n处理结果: " + processed;
            return CompletableFuture.completedFuture(Map.of("output", output));
        };
    }

    private String classifyContent(String input) {
        String lower = input.toLowerCase();
        if (containsAny(lower, "代码", "编程", "技术", "开发", "算法", "架构", "api", "框架", "bug")) {
            return "technical";
        }
        if (containsAny(lower, "天气", "旅游", "美食", "运动", "休闲", "玩", "好", "快乐")) {
            return "lifestyle";
        }
        return "general";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
