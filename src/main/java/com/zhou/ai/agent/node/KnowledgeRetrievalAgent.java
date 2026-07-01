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
public class KnowledgeRetrievalAgent {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeRetrievalAgent.class);

    private static final String FMT = """
            ## 用户需求
            %s

            ## 问题感知分析（前置Agent分析结果）
            %s

            请调用 retrieveKnowledge 工具检索相关金融知识，然后整理输出。
            """;

    private static final String FALLBACK = "【知识检索-降级】检索未返回结果，基于通用知识分析。";

    private final ReactAgentFactory factory;

    public KnowledgeRetrievalAgent(ReactAgentFactory factory) {
        this.factory = factory;
    }

    public AsyncNodeAction asNodeAction() {
        return state -> {
            AgentPipelineContext ctx = AgentPipelineContext.from(state);
            try {
                ReactAgent agent = factory.getKnowledgeRetrievalAgent(ctx.modelName());
                String result = agent.call(FMT.formatted(ctx.userMessage(), ctx.perceptionResult())).getText();
                if (result == null || result.isBlank()) result = FALLBACK;
                return CompletableFuture.completedFuture(Map.of(
                        AgentGraphState.RETRIEVAL_RESULT, result,
                        AgentGraphState.RETRIEVAL_STATUS, "completed"));
            } catch (Exception e) {
                log.warn("[知识检索Agent] 异常: {}", e.getMessage());
                return CompletableFuture.completedFuture(Map.of(
                        AgentGraphState.RETRIEVAL_RESULT, FALLBACK,
                        AgentGraphState.RETRIEVAL_STATUS, "completed"));
            }
        };
    }
}