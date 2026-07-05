package com.zhou.ai.agent.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.zhou.ai.agent.config.ReactAgentFactory;
import com.zhou.ai.agent.model.AgentGraphState;
import com.zhou.ai.agent.model.AgentPipelineContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 数据获取 Agent（第4步：ReAct 工具调用获取市场数据）。
 *
 * <p><b>模型策略：</b>通过 ReactAgentFactory.resolveToolModel() 自动切换到
 * {@code deepSeekChatModel}，因为 Agnes AI（openAiChatModel）在多工具调用场景
 * 下返回 404 NotFoundError。
 *
 * <p><b>降级策略：</b>
 * <ul>
 *   <li>NonTransientAiException（如上游 404）→ 记录专属警告，填充结构化降级数据</li>
 *   <li>其他异常 → 通用降级，不中断 Graph 流程</li>
 * </ul>
 *
 * @since 2026-06-30
 */
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

    /** 通用降级文本（无法区分原因时使用）。 */
    private static final String FALLBACK = """
            【数据获取-降级】无法获取市场实时数据，基于以下参考数据继续分析：
            - 科技板块：近30日上涨约3.5%%，波动率约18.5，VaR(95%%,30天)约-12.5%%
            - 半导体板块：近30日收益约2.9%%
            - 能源板块：近30日收益约4.7%%
            """;

    /** LLM 调用失败的专用降级文本。 */
    private static final String API_DOWN_FALLBACK = """
            【数据获取-降级】上游模型服务暂时不可用（HTTP 404），无法获取市场实时数据。
            基于以下参考数据继续分析：
            - 科技板块：近30日上涨约3.5%%
            - 半导体板块：近30日收益约2.9%%
            - 能源板块：近30日收益约4.7%%
            - VaR(95%%,30天,10万)≈ -3.5%%
            - 市场波动率约 18.5
            """;

    private final ReactAgentFactory factory;

    public DataFetchAgent(ReactAgentFactory factory) {
        this.factory = factory;
    }

    public AsyncNodeAction asNodeAction() {
        return state -> {
            AgentPipelineContext ctx = AgentPipelineContext.from(state);
            try {
                ReactAgent agent = factory.getDataFetchAgent(ctx.modelName());
                String result = agent.call(FMT.formatted(
                        ctx.userMessage(), ctx.perceptionResult(), ctx.retrievalResult())).getText();
                if (result == null || result.isBlank()) result = FALLBACK;
                return CompletableFuture.completedFuture(Map.of(
                        AgentGraphState.DATA_RESULT, result,
                        AgentGraphState.DATA_STATUS, "completed"));
            } catch (Exception e) {
                // 区分 NonTransientAiException（上游模型不可用，如 404）
                // 与其他异常（工具执行失败、超时等），输出不同级别的日志和降级信息
                if (isNonTransientAiError(e)) {
                    log.warn("[数据获取Agent] 上游模型服务不可用（Function Calling 可能不被支持）"
                            + " | model={} | 降级为占位数据", ctx.modelName(), e);
                    return CompletableFuture.completedFuture(Map.of(
                            AgentGraphState.DATA_RESULT, API_DOWN_FALLBACK,
                            AgentGraphState.DATA_STATUS, "completed"));
                }

                log.warn("[数据获取Agent] 工具调用异常 | model={} | 降级为占位数据",
                        ctx.modelName(), e);
                return CompletableFuture.completedFuture(Map.of(
                        AgentGraphState.DATA_RESULT, FALLBACK,
                        AgentGraphState.DATA_STATUS, "completed"));
            }
        };
    }

    /**
     * 判断异常是否由上游模型服务不可达（HTTP 404 等 NonTransientAiException）导致。
     *
     * <p>遍历异常因果链，检查是否存在 NonTransientAiException。
     * Spring AI 的 RetryTemplate 会将原始异常包装在 NonTransientAiException 中。
     */
    private boolean isNonTransientAiError(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof NonTransientAiException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
