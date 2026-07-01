package com.zhou.ai.graph.config;

import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.zhou.ai.agent.model.AgentGraphState;
import com.zhou.ai.agent.node.IntentClassifyAgent;
import com.zhou.ai.agent.node.ProblemPerceptionAgent;
import com.zhou.ai.agent.node.KnowledgeRetrievalAgent;
import com.zhou.ai.agent.node.DataFetchAgent;
import com.zhou.ai.agent.node.ReasoningAnalysisAgent;
import com.zhou.ai.agent.node.DecisionGenerateAgent;
import com.zhou.ai.agent.node.GraphScheduleAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Graph 工作流配置：定义投资决策的简化工作流，包含投资分析与结果输出。
 */
@Configuration
public class WorkflowGraphConfig {

    private static final Logger log = LoggerFactory.getLogger(WorkflowGraphConfig.class);

    @Bean
    public StateGraph workflowGraph() throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = () -> {
            Map<String, com.alibaba.cloud.ai.graph.KeyStrategy> strategies = new java.util.HashMap<>();
            strategies.put("input", new ReplaceStrategy());
            strategies.put("processedResult", new ReplaceStrategy());
            strategies.put("output", new ReplaceStrategy());
            return strategies;
        };

        return new StateGraph(keyStrategyFactory)
                .addNode("process_investment", processInvestmentNode())
                .addNode("output", outputNode())
                .addEdge(START, "process_investment")
                .addEdge("process_investment", "output")
                .addEdge("output", END);
    }

    private AsyncNodeAction processInvestmentNode() {
        return state -> {
            String input = (String) state.value("input").orElse("");
            String result = "【投资分析】" + input + " —— 已基于市场趋势、风险评估和资产配置角度进行分析。";
            return CompletableFuture.completedFuture(Map.of("processedResult", result));
        };
    }

    private AsyncNodeAction outputNode() {
        return state -> {
            String processed = (String) state.value("processedResult").orElse("");
            String output = "分析结果:\n" + processed;
            return CompletableFuture.completedFuture(Map.of("output", output));
        };
    }
    // ========================================================================
    // 以下为 multiAgent 模式专用 StateGraph Bean（全新7节点多Agent图编排架构）
    //
    // 【与原有两节点Graph的区别】
    //   原有两节点 Graph（workflowGraph）：
    //     - 仅包含 process_investment、output 两个硬编码无LLM节点
    //     - 用于基础工作流验证，无多智能体能力
    //     - 对应原有 decideStream 流程的第6步，作为可选补充
    //
    //   multiAgent Graph（multiAgentWorkflowGraph）：
    //     - 7个独立LLM Agent节点，一一对应完整6步业务+前置意图分类
    //     - 使用原生 addConditionalEdges 条件路由控制RAG、Tools动态跳过
    //     - 每个Agent为独立@Component，携带专属ChatClient和领域提示词
    //     - 通过 yml 配置 ai.graph.workflow.mode=multi-agent 激活
    //
    // 【适用场景】
    //   原有流程：流程固定6步，手写Flux链控制条件跳过，适合确定性流水线
    //   多Agent Graph：条件路由灵活，节点可扩展，适合动态编排和演示多Agent架构
    //
    //   ⚠️ 两套流程完全隔离：multiAgent模式不会调用原有 process_investment/output 节点
    // ========================================================================

    /**
     * 创建 multiAgent 模式专用 StateGraph Bean。
     *
     * <p>包含7个Agent节点，通过条件路由实现RAG/Tools动态跳过。
     * 通过 {@code ai.graph.workflow.mode=multi-agent} 配置激活。
     * 原有 workflowGraph Bean 不受影响。
     */
    @Bean("multiAgentWorkflowGraph")
    public StateGraph multiAgentWorkflowGraph(
            IntentClassifyAgent intentClassifyAgent,
            ProblemPerceptionAgent problemPerceptionAgent,
            KnowledgeRetrievalAgent knowledgeRetrievalAgent,
            DataFetchAgent dataFetchAgent,
            ReasoningAnalysisAgent reasoningAnalysisAgent,
            DecisionGenerateAgent decisionGenerateAgent,
            GraphScheduleAgent graphScheduleAgent) throws GraphStateException {

        log.info("初始化 multiAgent 模式 StateGraph（7节点多Agent图编排架构）");

        // 定义状态键策略：所有键使用 ReplaceStrategy（每个Agent写入独立键）
        KeyStrategyFactory keyStrategyFactory = () -> {
            Map<String, com.alibaba.cloud.ai.graph.KeyStrategy> strategies = new java.util.HashMap<>();
            strategies.put(AgentGraphState.USER_MESSAGE, new ReplaceStrategy());
            strategies.put(AgentGraphState.MODEL_NAME, new ReplaceStrategy());

            strategies.put(AgentGraphState.THREAD_ID, new ReplaceStrategy());
            strategies.put(AgentGraphState.START_TIME, new ReplaceStrategy());
            strategies.put(AgentGraphState.INTENT_RESULT, new ReplaceStrategy());
            strategies.put(AgentGraphState.IS_INVESTMENT, new ReplaceStrategy());
            strategies.put(AgentGraphState.PERCEPTION_RESULT, new ReplaceStrategy());
            strategies.put(AgentGraphState.RETRIEVAL_RESULT, new ReplaceStrategy());
            strategies.put(AgentGraphState.RETRIEVAL_STATUS, new ReplaceStrategy());
            strategies.put(AgentGraphState.DATA_RESULT, new ReplaceStrategy());
            strategies.put(AgentGraphState.DATA_STATUS, new ReplaceStrategy());
            strategies.put(AgentGraphState.REASONING_RESULT, new ReplaceStrategy());
            strategies.put(AgentGraphState.DECISION_RESULT, new ReplaceStrategy());
            strategies.put(AgentGraphState.SCHEDULE_RESULT, new ReplaceStrategy());
            strategies.put(AgentGraphState.CONTEXT, new ReplaceStrategy());
            strategies.put(AgentGraphState.RISK_WARNING, new ReplaceStrategy());
            return strategies;
        };

        StateGraph graph = new StateGraph(keyStrategyFactory);

        // ── 注册7个Agent节点 ──
        graph.addNode("intentClassify", intentClassifyAgent.asNodeAction());
        graph.addNode("problemPerception", problemPerceptionAgent.asNodeAction());
        graph.addNode("knowledgeRetrieval", knowledgeRetrievalAgent.asNodeAction());
        graph.addNode("dataFetch", dataFetchAgent.asNodeAction());
        graph.addNode("reasoningAnalysis", reasoningAnalysisAgent.asNodeAction());
        graph.addNode("decisionGenerate", decisionGenerateAgent.asNodeAction());
        graph.addNode("graphSchedule", graphScheduleAgent.asNodeAction());

        // ── 入口边 ──
        graph.addEdge(START, "intentClassify");

        // ── 意图分类 → 条件路由 ──
        // 如果是投资相关，进入问题感知；否则结束流程
        graph.addConditionalEdges(
                "intentClassify",
                (OverAllState state) -> {
                    String isInvestment = (String) state.value(AgentGraphState.IS_INVESTMENT).orElse("true");
                    return CompletableFuture.completedFuture(
                            "true".equals(isInvestment) ? "problemPerception" : END);
                },
                Map.of("problemPerception", "problemPerception", END, END)
        );

        // ── 固定串行链路：所有Agent节点按序执行 ──
        // 从问题感知开始，依次经过知识检索→数据获取→推理分析→决策生成→流程编排
        // Agent内部由LLM自主判断是否执行检索、工具调用，不需要外部参数控制
        graph.addEdge("problemPerception", "knowledgeRetrieval");
        graph.addEdge("knowledgeRetrieval", "dataFetch");
        graph.addEdge("dataFetch", "reasoningAnalysis");
        graph.addEdge("reasoningAnalysis", "decisionGenerate");
        graph.addEdge("decisionGenerate", "graphSchedule");
        graph.addEdge("graphSchedule", END);

        log.info("multiAgent StateGraph 构建完成：7个Agent节点，线性串行链路");

        return graph;
    }
}

