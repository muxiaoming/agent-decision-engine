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
public class ReasoningAnalysisAgent {

    private static final Logger log = LoggerFactory.getLogger(ReasoningAnalysisAgent.class);

    private static final String FMT = """
            ## 用户需求
            %s

            ## 问题感知分析
            %s

            ## 知识检索结果
            %s

            ## 市场数据
            %s

            请基于以上数据进行深入分析：市场趋势、风险因素、潜在收益、配置建议。
            """;

    private static final String FALLBACK = "【推理分析-降级】科技行业处于上升周期。科技股估值弹性大，短期波动风险较高。";

    private final ReactAgentFactory factory;

    public ReasoningAnalysisAgent(ReactAgentFactory factory) {
        this.factory = factory;
    }

    public AsyncNodeAction asNodeAction() {
        return state -> {
            AgentPipelineContext ctx = AgentPipelineContext.from(state);
            try {
                ReactAgent agent = factory.getReasoningAnalysisAgent(ctx.modelName());
                String result = agent.call(FMT.formatted(ctx.userMessage(), ctx.perceptionResult(), ctx.retrievalResult(), ctx.dataResult())).getText();
                if (result == null || result.isBlank()) result = FALLBACK;
                return CompletableFuture.completedFuture(Map.of(AgentGraphState.REASONING_RESULT, result));
            } catch (Exception e) {
                log.warn("[推理分析Agent] 异常: {}", e.getMessage(), e);
                return CompletableFuture.completedFuture(Map.of(AgentGraphState.REASONING_RESULT, FALLBACK));
            }
        };
    }
}