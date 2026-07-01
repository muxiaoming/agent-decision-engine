package com.zhou.ai.agent.service;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.zhou.ai.agent.model.AgentGraphState;
import com.zhou.ai.investment.model.InvestmentDecisionRequest;
import com.zhou.ai.investment.model.InvestmentStepEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;

/**
 * 多Agent投资决策服务 - 独立的多Agent Graph流式执行服务。
 *
 * <p><b>设计说明：</b>
 * 本服务使用7节点多Agent StateGraph执行完整的投资决策流程。
 * 7个Agent节点以线性串行链路固定排列，由Graph原生条件路由仅在意图分类
 * （非投资→END）处做分支控制，其余节点全部串行执行。
 *
 * <p><b>与原有{@code InvestmentDecisionService}的区别：</b>
 * <ul>
 *   <li><b>原有服务</b>：手写6步Flux链，enableRag/enableTools控制步骤跳转</li>
 *   <li><b>本服务</b>：7个Agent节点线性串行，LLM自主判断检索/工具调用需求，
 *       无需外部参数控制</li>
 * </ul>
 *
 * <p><b>事件格式对齐：</b>
 * 输出的{@code Flux<InvestmentStepEvent>}结构、字段、事件类型、步骤名称、
 * 最终决策事件格式与原有decideStream完全一致。
 *
 * @since 2026-06-30
 */
@Service
public class MultiAgentInvestService {

    private static final Logger log = LoggerFactory.getLogger(MultiAgentInvestService.class);

    private static final String RISK_WARNING_TEMPLATE = """
            \u26a0\uFE0F 风险提示
            \u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501
            1. 投资有风险，入市需谨慎
            2. 以上投资建议仅供参考，不构成投资建议
            3. 过往业绩不代表未来表现
            4. 请根据自身风险承受能力做出投资决策
            5. 如有疑问，请咨询专业投资顾问
            """;

    private final StateGraph multiAgentWorkflowGraph;

    private CompiledGraph compiledGraph;

    public MultiAgentInvestService(
            @Qualifier("multiAgentWorkflowGraph") StateGraph multiAgentWorkflowGraph) {
        this.multiAgentWorkflowGraph = multiAgentWorkflowGraph;
    }

    @PostConstruct
    public void init() throws GraphStateException {
        log.info("编译 multiAgent StateGraph...");
        this.compiledGraph = multiAgentWorkflowGraph.compile();
        log.info("multiAgent StateGraph 编译完成");
    }

    /**
     * 流式执行多Agent投资决策流程。
     *
     * <p>事件序列与原有decideStream完全对齐：
     * <ol>
     *   <li>step1: 意图分类</li>
     *   <li>step2: 问题感知</li>
     *   <li>step3: 知识检索</li>
     *   <li>step4: 数据获取</li>
     *   <li>step5: 推理分析</li>
     *   <li>step6: 决策生成</li>
     *   <li>step7: 汇总输出</li>
     *   <li>decision_complete: 最终事件</li>
     * </ol>
     */
    public Flux<InvestmentStepEvent> executeStream(InvestmentDecisionRequest request) {
        long startTime = System.currentTimeMillis();
        String threadId = request.effectiveThreadId();

        log.info("开始multiAgent线性串行决策: threadId={}", threadId);

        return Flux.defer(() -> {
            try {
                // ── 构建初始状态并执行Graph（一次invoke执行全部节点） ──
                Map<String, Object> initialState = buildInitialState(
                        request.message(), request.effectiveModelName(), threadId, startTime);

                Optional<OverAllState> result = compiledGraph.invoke(initialState,
                        RunnableConfig.builder().build());

                if (result.isEmpty()) {
                    return Flux.just(InvestmentStepEvent.error("Graph执行无返回结果"));
                }

                OverAllState state = result.get();
                long totalDuration = System.currentTimeMillis() - startTime;

                // 读取isInvestment状态
                String isInvestment = (String) state.value(AgentGraphState.IS_INVESTMENT).orElse("true");
                boolean isInvest = "true".equals(isInvestment);

                if (!isInvest) {
                    // ── 非投资消息：仅执行意图分类 ──
                    log.info("非投资消息，直接回复");
                    String chatReply = "您好，我是投资决策助手，专注于金融投资相关问题。" +
                            "如果您有任何投资理财方面的问题，欢迎随时提问！";
                    return Flux.just(
                            InvestmentStepEvent.stepStart(1, "意图分类", "agent"),
                            InvestmentStepEvent.stepComplete(1, "意图分类", "agent", chatReply),
                            InvestmentStepEvent.decisionComplete(threadId, chatReply, null, totalDuration)
                    );
                }

                // ── 投资消息：读取所有Agent结果 ──
                String intentResult = readState(state, AgentGraphState.INTENT_RESULT, "意图分类完成");
                String perceptionResult = readState(state, AgentGraphState.PERCEPTION_RESULT, "问题感知完成");
                String retrievalResult = readState(state, AgentGraphState.RETRIEVAL_RESULT, "知识检索完成");
                String dataResult = readState(state, AgentGraphState.DATA_RESULT, "数据获取完成");
                String reasoningResult = readState(state, AgentGraphState.REASONING_RESULT, "推理分析完成");
                String decisionResult = readState(state, AgentGraphState.DECISION_RESULT, "决策生成完成");
                String scheduleResult = readState(state, AgentGraphState.SCHEDULE_RESULT, "汇总输出完成");

                log.info("Graph执行完成，7个Agent结果均已读取");

                // ── 构建事件序列 ──
                String finalAdvice = decisionResult != null ? decisionResult : "投资决策已完成，请查看各步骤详情";

                return Flux.just(
                        // 步骤1: 意图分类
                        InvestmentStepEvent.stepStart(1, "意图分类", "agent"),
                        InvestmentStepEvent.stepComplete(1, "意图分类", "agent", intentResult),
                        // 步骤2: 问题感知
                        InvestmentStepEvent.stepStart(2, "问题感知", "agent"),
                        InvestmentStepEvent.stepComplete(2, "问题感知", "agent", perceptionResult),
                        // 步骤3: 知识检索
                        InvestmentStepEvent.stepStart(3, "知识检索", "agent"),
                        InvestmentStepEvent.stepComplete(3, "知识检索", "agent", retrievalResult),
                        // 步骤4: 数据获取
                        InvestmentStepEvent.stepStart(4, "数据获取", "agent"),
                        InvestmentStepEvent.stepComplete(4, "数据获取", "agent", dataResult),
                        // 步骤5: 推理分析
                        InvestmentStepEvent.stepStart(5, "推理分析", "agent"),
                        InvestmentStepEvent.stepComplete(5, "推理分析", "agent", reasoningResult),
                        // 步骤6: 决策生成
                        InvestmentStepEvent.stepStart(6, "决策生成", "agent"),
                        InvestmentStepEvent.stepComplete(6, "决策生成", "agent", decisionResult),
                        // 步骤7: 汇总输出
                        InvestmentStepEvent.stepStart(7, "汇总输出", "agent"),
                        InvestmentStepEvent.stepComplete(7, "汇总输出", "agent", scheduleResult),
                        // 最终完成事件
                        InvestmentStepEvent.decisionComplete(
                                threadId, finalAdvice, RISK_WARNING_TEMPLATE, totalDuration)
                );

            } catch (Exception e) {
                log.error("multiAgent流程异常: {}", e.getMessage(), e);
                return Flux.just(InvestmentStepEvent.error("流程异常: " + e.getMessage()));
            }
        });
    }

    /**
     * 安全地从OverAllState读取指定key的值。
     */
    private String readState(OverAllState state, String key, String fallback) {
        return (String) state.value(key).orElse("【降级】" + fallback);
    }

    /**
     * 构建Graph初始状态。
     */
    private Map<String, Object> buildInitialState(String userMessage, String modelName,
                                                   String threadId, long startTime) {
        return Map.ofEntries(
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
        );
    }
}
