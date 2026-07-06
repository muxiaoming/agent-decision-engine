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
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.concurrent.Executors;

/**
 * 多Agent投资决策服务 - 独立的多Agent Graph流式执行服务。
 *
 * <p><b>设计说明：</b>
 * 本服务手动串联7个Agent节点的{@link AsyncNodeAction}，使用
 * {@link Flux#concat(org.reactivestreams.Publisher[])} + {@link Flux#merge(org.reactivestreams.Publisher[])}
 * 实现逐节点流式推送 + 无依赖步骤并行执行。
 *
 * <p><b>并行策略：</b>
 * <ul>
 *   <li>Step 1 → Step 2 串行（意图分类作为守门，非投资消息即时返回）</li>
 *   <li>Steps 3+4 并行（知识检索 ‖ 数据获取，均仅依赖 perception_result）</li>
 *   <li>Steps 5→6→7 严格串行（逐级数据依赖）</li>
 * </ul>
 *
 * <pre>
 *   Step1(意图分类) → [isInvestment?] → Step2(问题感知) → Step3(知识检索) ─┬─→ Step5 → Step6 → Step7
 *                                                          Step4(数据获取) ┘
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

    /** 虚拟线程调度器。 */
    private static final Scheduler VT_SCHEDULER =
            Schedulers.fromExecutor(Executors.newVirtualThreadPerTaskExecutor());

    private final IntentClassifyAgent intentClassifyAgent;
    private final ProblemPerceptionAgent problemPerceptionAgent;
    private final KnowledgeRetrievalAgent knowledgeRetrievalAgent;
    private final DataFetchAgent dataFetchAgent;
    private final ReasoningAnalysisAgent reasoningAnalysisAgent;
    private final DecisionGenerateAgent decisionGenerateAgent;
    private final GraphScheduleAgent graphScheduleAgent;
    private final ObservationRegistry observationRegistry;

    public MultiAgentInvestService(
            IntentClassifyAgent intentClassifyAgent,
            ProblemPerceptionAgent problemPerceptionAgent,
            KnowledgeRetrievalAgent knowledgeRetrievalAgent,
            DataFetchAgent dataFetchAgent,
            ReasoningAnalysisAgent reasoningAnalysisAgent,
            DecisionGenerateAgent decisionGenerateAgent,
            GraphScheduleAgent graphScheduleAgent,
            ObservationRegistry observationRegistry) {
        this.intentClassifyAgent = intentClassifyAgent;
        this.problemPerceptionAgent = problemPerceptionAgent;
        this.knowledgeRetrievalAgent = knowledgeRetrievalAgent;
        this.dataFetchAgent = dataFetchAgent;
        this.reasoningAnalysisAgent = reasoningAnalysisAgent;
        this.decisionGenerateAgent = decisionGenerateAgent;
        this.graphScheduleAgent = graphScheduleAgent;
        this.observationRegistry = observationRegistry;
        log.info("MultiAgentInvestService 初始化：7个Agent节点已注入，Steps 3+4 并行");
    }

    /**
     * 流式执行多Agent投资决策流程 —— 串行守门 + 并行加速 + 串行收尾。
     *
     * <p>执行拓扑：
     * <pre>
     *   Step1(意图分类) → [isInvestment?]
     *                       ├─ false → decisionComplete(拒)
     *                       └─ true  → Step2(问题感知)
     *                                   → Step3(知识检索) ─┬─→ Step5 → Step6 → Step7 → complete
     *                                      Step4(数据获取) ┘
     * </pre>
     */
    public Flux<InvestmentStepEvent> executeStream(InvestmentDecisionRequest request) {
        long startTime = System.currentTimeMillis();
        String threadId = request.effectiveThreadId();

        log.info("开始multiAgent流式决策（虚拟线程 + Steps 3+4 并行）: threadId={}", threadId);

        Map<String, Object> state = buildInitialState(
                request.message(), request.effectiveModelName(), threadId, startTime);

        // ── Step 1: 意图分类（守门，必须串行先跑） ──
        Flux<InvestmentStepEvent> step1 = executeStep(
                state, intentClassifyAgent.asNodeAction(),
                1, "意图分类", AgentGraphState.INTENT_RESULT);

        // 捕获当前 HTTP 请求线程的 OTel span（Observation），写入 Reactor Context。
        // Hooks.enableAutomaticContextPropagation() 在 subscribeOn 切换调度器时
        // 自动恢复 Observation 到虚拟线程，使 Langfuse 链路保持单一 traceId。
        Observation currentObservation = observationRegistry.getCurrentObservation();

        Flux<InvestmentStepEvent> pipeline = step1.concatWith(Flux.defer(() -> {
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
                    // Step 2: 问题感知（Steps 3-4 的前置依赖，必须串行）
                    executeStep(state, problemPerceptionAgent.asNodeAction(),
                            2, "问题感知", AgentGraphState.PERCEPTION_RESULT),

                    // Steps 3+4 并行执行 + 有序输出（先步骤3，再步骤4）
                    executeParallelStepsOrdered(state,
                            knowledgeRetrievalAgent.asNodeAction(), 3, "知识检索", AgentGraphState.RETRIEVAL_RESULT,
                            dataFetchAgent.asNodeAction(), 4, "数据获取", AgentGraphState.DATA_RESULT),

                    // Steps 5→6→7 严格串行（逐级数据依赖）
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

        if (currentObservation != null) {
            pipeline = pipeline.contextWrite(ctx -> ctx.put(Observation.class, currentObservation));
        }

        return pipeline;
    }

    // ==================== 步骤执行 ====================

    /**
     * 执行单个Agent节点，返回{@code stepStart + stepComplete}事件流。
     *
     * <p>阻塞LLM调用通过 {@link #VT_SCHEDULER} 调度到虚拟线程执行。
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

            return Flux.concat(
                    Flux.just(InvestmentStepEvent.stepStart(stepNum, displayName, MODULE_NAME)),
                    Mono.fromCallable(() -> action.apply(new OverAllState(state)).get())
                            .subscribeOn(VT_SCHEDULER)
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

    /**
     * 并行执行两个Agent节点，但按编号顺序输出事件。
     *
     * <p>两步骤的 {@code stepStart} 按顺序立即发出，然后两个 LLM 调用通过
     * {@link Mono#zip} 并行执行，都完成后依次发出 {@code stepComplete}。
     *
     * <p>任一故障不会影响另一步骤（使用独立的 {@code onErrorResume} 降级），
     * 故障步骤会发出 {@code stepError}。
     */
    private Flux<InvestmentStepEvent> executeParallelStepsOrdered(
            Map<String, Object> state,
            AsyncNodeAction actionA, int stepNumA, String nameA, String resultKeyA,
            AsyncNodeAction actionB, int stepNumB, String nameB, String resultKeyB) {

        long startA = System.currentTimeMillis();
        long startB = System.currentTimeMillis();

        log.info("Step {} ({}) 开始执行", stepNumA, nameA);
        log.info("Step {} ({}) 开始执行", stepNumB, nameB);

        Mono<Map<String, Object>> actionAMono = Mono.fromCallable(
                () -> actionA.apply(new OverAllState(state)).get())
                .subscribeOn(VT_SCHEDULER)
                .doOnSuccess(r -> log.info("Step {} ({}) 完成，耗时={}ms",
                        stepNumA, nameA, System.currentTimeMillis() - startA))
                .onErrorResume(e -> {
                    log.warn("Step {} ({}) 失败: {}", stepNumA, nameA, e.getMessage());
                    return Mono.just(Map.of(resultKeyA, "【降级】" + nameA + "异常: " + e.getMessage()));
                });

        Mono<Map<String, Object>> actionBMono = Mono.fromCallable(
                () -> actionB.apply(new OverAllState(state)).get())
                .subscribeOn(VT_SCHEDULER)
                .doOnSuccess(r -> log.info("Step {} ({}) 完成，耗时={}ms",
                        stepNumB, nameB, System.currentTimeMillis() - startB))
                .onErrorResume(e -> {
                    log.warn("Step {} ({}) 失败: {}", stepNumB, nameB, e.getMessage());
                    return Mono.just(Map.of(resultKeyB, "【降级】" + nameB + "异常: " + e.getMessage()));
                });

        return Flux.concat(
                // stepStart 按顺序立即发出
                Flux.just(InvestmentStepEvent.stepStart(stepNumA, nameA, MODULE_NAME)),
                Flux.just(InvestmentStepEvent.stepStart(stepNumB, nameB, MODULE_NAME)),

                // 两个 action 并行执行（zip 同时订阅两个 Mono），结果按顺序发出
                Mono.zip(actionAMono, actionBMono).flatMapMany(tuple -> {
                    Map<String, Object> resultA = tuple.getT1();
                    Map<String, Object> resultB = tuple.getT2();

                    state.putAll(resultA);
                    String finalResultA = (String) resultA.getOrDefault(
                            resultKeyA, "【降级】" + nameA + "完成");
                    Flux<InvestmentStepEvent> eventA = finalResultA.startsWith("【降级】")
                            ? Flux.just(InvestmentStepEvent.stepError(
                                    stepNumA, nameA, MODULE_NAME, finalResultA))
                            : Flux.just(InvestmentStepEvent.stepComplete(
                                    stepNumA, nameA, MODULE_NAME, finalResultA));

                    state.putAll(resultB);
                    String finalResultB = (String) resultB.getOrDefault(
                            resultKeyB, "【降级】" + nameB + "完成");
                    Flux<InvestmentStepEvent> eventB = finalResultB.startsWith("【降级】")
                            ? Flux.just(InvestmentStepEvent.stepError(
                                    stepNumB, nameB, MODULE_NAME, finalResultB))
                            : Flux.just(InvestmentStepEvent.stepComplete(
                                    stepNumB, nameB, MODULE_NAME, finalResultB));

                    return Flux.concat(eventA, eventB);
                })
        );
    }

    // ==================== 辅助方法 ====================

    private InvestmentStepEvent buildDecisionComplete(
            String threadId, String finalAdvice, String riskWarning, long startTime) {
        long totalDuration = System.currentTimeMillis() - startTime;
        log.info("multiAgent流式决策完成: 耗时={}ms", totalDuration);
        return InvestmentStepEvent.decisionComplete(threadId, finalAdvice, riskWarning, totalDuration);
    }

    /**
     * 构建 Graph 初始状态（ConcurrentHashMap 支持并行步骤并发写入）。
     */
    private Map<String, Object> buildInitialState(String userMessage, String modelName,
                                                   String threadId, long startTime) {
        return new java.util.concurrent.ConcurrentHashMap<>(Map.ofEntries(
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
