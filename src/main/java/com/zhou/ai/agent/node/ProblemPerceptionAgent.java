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
public class ProblemPerceptionAgent {

    private static final Logger log = LoggerFactory.getLogger(ProblemPerceptionAgent.class);

    private static final String FMT = """
            用户输入:
            %s
            """;

    private static final String FALLBACK = "【问题感知-降级】未能获取AI响应，基于用户输入继续分析。";

    private final ReactAgentFactory factory;

    public ProblemPerceptionAgent(ReactAgentFactory factory) {
        this.factory = factory;
    }

    public AsyncNodeAction asNodeAction() {
        return state -> {
            AgentPipelineContext ctx = AgentPipelineContext.from(state);
            try {
                ReactAgent agent = factory.getProblemPerceptionAgent(ctx.modelName());
                String result = agent.call(FMT.formatted(ctx.userMessage())).getText();
                if (result == null || result.isBlank()) result = FALLBACK;
                return CompletableFuture.completedFuture(Map.of(AgentGraphState.PERCEPTION_RESULT, result));
            } catch (Exception e) {
                log.warn("[问题感知Agent] 异常: {}", e.getMessage());
                return CompletableFuture.completedFuture(Map.of(AgentGraphState.PERCEPTION_RESULT, FALLBACK));
            }
        };
    }
}