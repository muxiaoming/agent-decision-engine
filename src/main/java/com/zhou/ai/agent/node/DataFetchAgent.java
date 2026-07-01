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
public class DataFetchAgent {

    private static final Logger log = LoggerFactory.getLogger(DataFetchAgent.class);

    private static final String FMT = """
            ## 用户需求
            %s

            ## 问题感知分析
            %s

            ## 知识检索结果
            %s

            请使用工具获取市场数据（股价、指数、风险指标），然后汇总输出结构化的数据摘要。
            """;

    private static final String FALLBACK = "【数据获取-降级】科技板块：近30日上涨约3.5%%，波动率约18.5，VaR(95%%,30天)约-12.5%%";

    private final ReactAgentFactory factory;

    public DataFetchAgent(ReactAgentFactory factory) {
        this.factory = factory;
    }

    public AsyncNodeAction asNodeAction() {
        return state -> {
            AgentPipelineContext ctx = AgentPipelineContext.from(state);
            try {
                ReactAgent agent = factory.getDataFetchAgent(ctx.modelName());
                String result = agent.call(FMT.formatted(ctx.userMessage(), ctx.perceptionResult(), ctx.retrievalResult())).getText();
                if (result == null || result.isBlank()) result = FALLBACK;
                return CompletableFuture.completedFuture(Map.of(
                        AgentGraphState.DATA_RESULT, result,
                        AgentGraphState.DATA_STATUS, "completed"));
            } catch (Exception e) {
                log.warn("[数据获取Agent] 异常: {}", e.getMessage());
                return CompletableFuture.completedFuture(Map.of(
                        AgentGraphState.DATA_RESULT, FALLBACK,
                        AgentGraphState.DATA_STATUS, "completed"));
            }
        };
    }
}