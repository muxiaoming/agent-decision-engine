package com.zhou.ai.agent.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.zhou.ai.agent.config.ReactAgentFactory;
import com.zhou.ai.agent.model.AgentGraphState;
import com.zhou.ai.agent.model.AgentPipelineContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class GraphScheduleAgent {

    private static final Logger log = LoggerFactory.getLogger(GraphScheduleAgent.class);

    private static final String FMT = """
            ## 各Agent执行结果

            ### 问题感知
            %s

            ### 知识检索
            %s

            ### 市场数据
            %s

            ### 推理分析
            %s

            ### 决策建议
            %s

            请汇总验证流程完整性和信息一致性，输出最终决策报告。
            """;

    private static final String FALLBACK = "【流程编排-降级】决策流程已完成。";

    private final ReactAgentFactory factory;

    public GraphScheduleAgent(ReactAgentFactory factory) {
        this.factory = factory;
    }

    public AsyncNodeAction asNodeAction() {
        return state -> {
            AgentPipelineContext ctx = AgentPipelineContext.from(state);
            try {
                ReactAgent agent = factory.getGraphScheduleAgent(ctx.modelName());
                String result = agent.call(FMT.formatted(ctx.perceptionResult(), ctx.retrievalResult(), ctx.dataResult(), ctx.reasoningResult(), ctx.decisionResult())).getText();
                if (result == null || result.isBlank()) result = FALLBACK;
                return CompletableFuture.completedFuture(Map.of(AgentGraphState.SCHEDULE_RESULT, result));
            } catch (Exception e) {
                log.warn("[流程编排Agent] 异常: {}", e.getMessage());
                return CompletableFuture.completedFuture(Map.of(AgentGraphState.SCHEDULE_RESULT, FALLBACK));
            }
        };
    }
}