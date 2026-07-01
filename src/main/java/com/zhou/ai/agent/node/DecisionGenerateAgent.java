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
public class DecisionGenerateAgent {

    private static final Logger log = LoggerFactory.getLogger(DecisionGenerateAgent.class);

    private static final String FMT = """
            ## 用户需求
            %s

            ## 问题感知分析
            %s

            ## 推理分析结果
            %s

            请生成最终投资建议：策略总结、具体操作、风控措施、持有期限。
            """;

    private static final String FALLBACK = "【决策生成-降级】平衡型配置：股票约55%%，债券约30%%，现金约15%%。";

    private final ReactAgentFactory factory;

    public DecisionGenerateAgent(ReactAgentFactory factory) {
        this.factory = factory;
    }

    public AsyncNodeAction asNodeAction() {
        return state -> {
            AgentPipelineContext ctx = AgentPipelineContext.from(state);
            try {
                ReactAgent agent = factory.getDecisionGenerateAgent(ctx.modelName());
                String result = agent.call(FMT.formatted(ctx.userMessage(), ctx.perceptionResult(), ctx.reasoningResult())).getText();
                if (result == null || result.isBlank()) result = FALLBACK;
                return CompletableFuture.completedFuture(Map.of(AgentGraphState.DECISION_RESULT, result));
            } catch (Exception e) {
                log.warn("[决策生成Agent] 异常: {}", e.getMessage());
                return CompletableFuture.completedFuture(Map.of(AgentGraphState.DECISION_RESULT, FALLBACK));
            }
        };
    }
}