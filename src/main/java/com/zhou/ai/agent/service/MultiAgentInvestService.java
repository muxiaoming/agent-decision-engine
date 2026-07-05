package com.zhou.ai.agent.service;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.zhou.ai.agent.model.AgentGraphState;
import com.zhou.ai.agent.node.DataFetchAgent;
import com.zhou.ai.agent.node.DecisionGenerateAgent;
import com.zhou.ai.agent.node.GraphScheduleAgent;
import com.zhou.ai.agent.node.IntentClassifyAgent;
import com.zhou.ai.agent.node.KnowledgeRetrievalAgent;
import com.zhou.ai.agent.node.ProblemPerceptionAgent;
import com.zhou.ai.agent.node.ReasoningAnalysisAgent;
import com.zhou.ai.investment.model.InvestmentDecisionRequest;
import com.zhou.ai.investment.model.InvestmentStepEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

/**
 * 多Agent投资决策服务 - 独立的多Agent Graph流式执行服务。
 *
 * <p><b>设计说明：</b>
 * 本服务手动串联7个Agent节点的{@link AsyncNodeAction}，使用
 * {@link Flux#concat(org.reactivestreams.Publisher[])} + {@link Flux#defer(java.util.function.Supplier)}
 * 实现真正的逐节点流式推送，确保前端能获得打字机效果。
 *
 * <p><b>为什么要手动串联：</b>
 * {@code CompiledGraph.stream()} 虽然返回 {@code Flux<NodeOutput>}，
 * 但每个Agent的{@code AsyncNodeAction}内部同步阻塞等待LLM响应后返回已完成Future，
 * 导致{@code GraphRunner}看到所有节点瞬间完成，所有NodeOutput几乎同时emit。
 * 手动{@code Flux.concat()}链保证每个步骤的阻塞调用在上一步事件推送完成之后才订阅执行。
 *
 * <p><b>7节点执行路径：</b>
 * <pre>
 *   intentClassify → problemPerception → knowledgeRetrieval
 *   → dataFetch → reasoningAnalysis → decisionGenerate
 *   → graphSchedule → decisionComplete
 * </pre>
 *
 * @since 2026-06-30
 */
@Service
public class MultiAgentInvestService {

    private static final Logger log = LoggerFactory.getLogger(MultiAgentInvestService.class);

    private static final String RISK_WARNING_TEMPLATE = """
            ⚠️ 风险提示
            ━━━━━━━━━━━━━━━
            1. 投资有风险，入市需谨慎
            2. 以上投资建议仅供参考，不构成投资建议
            3. 过往业绩不代表未来表现
            4. 请根据自身风险承受能力做出投资决策
            5. 如有疑问，请咨询专业投资顾问
            """;

    private static final String MODULE_NAME = "agent";

    private final IntentClassifyAgent intentClassifyAgent;
    private final ProblemPerceptionAgent problemPerceptionAgent;
    private final KnowledgeRetrievalAgent knowledgeRetrievalAgent;
    private final DataFetchAgent dataFetchAgent;
    private final ReasoningAnalysisAgent reasoningAnalysisAgent;
    private final DecisionGenerateAgent decisionGenerateAgent;
    private final GraphScheduleAgent graphScheduleAgent;

    public MultiAgentInvestService(
            IntentClassifyAgent intentClassifyAgent,
            ProblemPerceptionAgent problemPerceptionAgent,
            KnowledgeRetrievalAgent knowledgeRetrievalAgent,
            DataFetchAgent dataFetchAgent,
            ReasoningAnalysisAgent reasoningAnalysisAgent,
            DecisionGenerateAgent decisionGenerateAgent,
            GraphScheduleAgent graphScheduleAgent) {
        this.intentClassifyAgent = intentClassifyAgent;
        this.problemPerceptionAgent = problemPerceptionAgent;
        this.knowledgeRetrievalAgent = knowledgeRetrievalAgent;
        this.dataFetchAgent = dataFetchAgent;
        this.reasoningAnalysisAgent = reasoningAnalysisAgent;
        this.decisionGenerateAgent = decisionGenerateAgent;
        this.graphScheduleAgent = graphScheduleAgent;
        log.info("MultiAgentInvestService 初始化：7个Agent节点已注入，流式执行模式");
    }

    /**
     * 流式执行多Agent投资决策流程 —— 真正的逐节点推送。
     *
     * <p>通过{@link Flux#concat}串联各Agent节点的{@link AsyncNodeAction}：
     * <ol>
     *   <li>意图分类 → 立即推送 step1 事件</li>
     *   <li>问题感知 → 立即推送 step2 事件</li>
     *   <li>知识检索 → 立即推送 step3 事件</li>
     *   <li>数据获取 → 立即推送 step4 事件</li>
     *   <li>推理分析 → 立即推送 step5 事件</li>
     *   <li>决策生成 → 立即推送 step6 事件</li>
     *   <li>汇总输出 → 立即推送 step7 事件</li>
     *   <li>流程结束 → 推送 decision_complete 事件</li>
     * </ol>
     */
    public Flux<InvestmentStepEvent> executeStream(InvestmentDecisionRequest request) {
        long startTime = System.currentTimeMillis();
        String threadId = request.effectiveThreadId();

        log.info("开始multiAgent流式决策（手动Flux链模式）: threadId={}", threadId);

        Map<String, Object> state = buildInitialState(
                request.message(), request.effectiveModelName(), threadId, startTime);

        // ── Step 1: 意图分类（总是执行） ──
        Flux<InvestmentStepEvent> step1 = executeStep(
                state, intentClassifyAgent.asNodeAction(),
                1, "意图分类", AgentGraphState.INTENT_RESULT);

        // ── Step 1 完成后动态决定后续步骤 ──
        return step1.concatWith(Flux.defer(() -> {
            boolean isInvestment = "true".equals(
                    (String) state.getOrDefault(AgentGraphState.IS_INVESTMENT, "true"));

            if (!isInvestment) {
                log.info("非投资类消息，跳过Step 2-7，直接返回");
                return Mono.fromCallable(() -> buildDecisionComplete(threadId,
                        "您好，我是投资决策助手，专注于金融投资相关问题。"
                                + "如果您有任何投资理财方面的问题，欢迎随时提问！",
                        null, startTime));
            }

            return Flux.concat(
                    executeStep(state, problemPerceptionAgent.asNodeAction(),
                            2, "问题感知", AgentGraphState.PERCEPTION_RESULT),
                    executeStep(state, knowledgeRetrievalAgent.asNodeAction(),
                            3, "知识检索", AgentGraphState.RETRIEVAL_RESULT),
                    executeStep(state, dataFetchAgent.asNodeAction(),
                            4, "数据获取", AgentGraphState.DATA_RESULT),
                    executeStep(state, reasoningAnalysisAgent.asNodeAction(),
                            5, "推理分析", AgentGraphState.REASONING_RESULT),
                    executeStep(state, decisionGenerateAgent.asNodeAction(),
                            6, "决策生成", AgentGraphState.DECISION_RESULT),
                    executeStep(state, graphScheduleAgent.asNodeAction(),
                            7, "汇总输出", AgentGraphState.SCHEDULE_RESULT),
                    Mono.fromCallable(() -> buildDecisionComplete(threadId,
                            "投资决策已完成，请查看各步骤详情",
                            RISK_WARNING_TEMPLATE, startTime))
            );
        }));
    }

    // ==================== 步骤执行 ====================

    /**
     * 执行单个Agent节点，返回{@code stepStart + stepComplete}事件流。
     *
     * <p>使用{@link Flux#defer}确保阻塞LLM调用在订阅时才执行，
     * 配合外层{@link Flux#concat}实现真正的逐步骤推送。
     *
     * @param state       运行状态Map（会被本方法修改）
     * @param action      节点的AsyncNodeAction
     * @param stepNum     步骤编号
     * @param displayName 步骤显示名称
     * @param resultKey   结果写入state的key
     */
    private Flux<InvestmentStepEvent> executeStep(
            Map<String, Object> state,
            AsyncNodeAction action,
            int stepNum,
            String displayName,
            String resultKey) {

        return Flux.defer(() -> {
            log.info("Step {} ({}) 开始执行", stepNum, displayName);
            long stepStart = System.currentTimeMillis();

            // stepStart 在当前 NIO 线程立即 emit → Netty 写入 buffer
            // 用 subscribeOn(boundedElastic) 把阻塞 LLM 调用踢到 worker 线程
            // → NIO 线程释放，Netty 自动 flush buffer → 前端立即收到 stepStart
            return Flux.concat(
                    Flux.just(InvestmentStepEvent.stepStart(stepNum, displayName, MODULE_NAME)),
                    Mono.fromCallable(() -> action.apply(new OverAllState(state)).get())
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMapMany(updateMap -> {
                                state.putAll(updateMap);
                                String result = (String) updateMap.getOrDefault(
                                        resultKey, "【降级】" + displayName + "完成");
                                long stepDuration = System.currentTimeMillis() - stepStart;
                                log.info("Step {} ({}) 完成，耗时={}ms", stepNum, displayName, stepDuration);
                                return Flux.just(InvestmentStepEvent.stepComplete(
                                        stepNum, displayName, MODULE_NAME, result));
                            })
                            .onErrorResume(e -> {
                                log.warn("Step {} ({}) 失败: {}", stepNum, displayName, e.getMessage());
                                String fallback = "【降级】" + displayName + "异常: " + e.getMessage();
                                state.put(resultKey, fallback);
                                return Flux.just(InvestmentStepEvent.stepError(
                                        stepNum, displayName, MODULE_NAME, e.getMessage()));
                            })
            );
        });
    }

    // ==================== 辅助方法 ====================

    private InvestmentStepEvent buildDecisionComplete(
            String threadId, String finalAdvice, String riskWarning, long startTime) {
        long totalDuration = System.currentTimeMillis() - startTime;
        log.info("multiAgent流式决策完成: 耗时={}ms", totalDuration);
        return InvestmentStepEvent.decisionComplete(threadId, finalAdvice, riskWarning, totalDuration);
    }

    /**
     * 构建 Graph 初始状态。
     */
    private Map<String, Object> buildInitialState(String userMessage, String modelName,
                                                   String threadId, long startTime) {
        return new java.util.HashMap<>(Map.ofEntries(
                Map.entry(AgentGraphState.USER_MESSAGE, userMessage),
                Map.entry(AgentGraphState.MODEL_NAME, modelName),
                Map.entry(AgentGraphState.THREAD_ID, threadId),
                Map.entry(AgentGraphState.START_TIME, String.valueOf(startTime)),
                Map.entry(AgentGraphState.RISK_WARNING, RISK_WARNING_TEMPLATE),
                Map.entry(AgentGraphState.CONTEXT, ""),
                Map.entry(AgentGraphState.INTENT_RESULT, ""),
                Map.entry(AgentGraphState.IS_INVESTMENT, "true"),
                Map.entry(AgentGraphState.PERCEPTION_RESULT, ""),
                Map.entry(AgentGraphState.RETRIEVAL_RESULT, ""),
                Map.entry(AgentGraphState.RETRIEVAL_STATUS, "skipped"),
                Map.entry(AgentGraphState.DATA_RESULT, ""),
                Map.entry(AgentGraphState.DATA_STATUS, "skipped"),
                Map.entry(AgentGraphState.REASONING_RESULT, ""),
                Map.entry(AgentGraphState.DECISION_RESULT, ""),
                Map.entry(AgentGraphState.SCHEDULE_RESULT, "")
        ));
    }
}
