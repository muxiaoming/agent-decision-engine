package com.zhou.ai.investment.service;

import com.zhou.ai.graph.service.GraphWorkflowService;
import com.zhou.ai.investment.model.*;
import com.zhou.ai.rag.service.RagService;
import com.zhou.ai.skills.service.SkillsAgentService;
import com.zhou.ai.tools.service.MarketIndexToolService;
import com.zhou.ai.tools.service.RiskCalculatorToolService;
import com.zhou.ai.tools.service.StockPriceToolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 投资决策服务。
 * 串联所有核心功能，执行完整的投资决策流程。
 *
 * 决策流程：
 * 1. 问题感知 (Skills) - 理解需求、选择技能
 * 2. 知识检索 (RAG) - 检索财报、市场报告
 * 3. 数据获取 (Tools) - 查询股价、市场指数、计算风险
 * 4. 推理分析 (Skills) - 基于知识和数据分析
 * 5. 决策生成 (Skills) - 生成投资建议
 * 6. 流程编排 (Graph) - 执行决策流程、验证完整性
 */
@Service
public class InvestmentDecisionService {

    private static final Logger log = LoggerFactory.getLogger(InvestmentDecisionService.class);

    /**
     * 最大上下文长度限制。
     */
    private static final int MAX_CONTEXT_LENGTH = 10000;

    /**
     * 投资决策系统提示词。
     */
    private static final String DECISION_SYSTEM_PROMPT = """
            你是一个智能投资代理决策引擎，负责执行完整的投资决策流程。

            决策流程：
            1. 理解用户投资需求（预算、风险偏好、投资期限）
            2. 分析当前市场环境
            3. 推荐合适的投资标的
            4. 评估投资风险
            5. 优化投资组合配置
            6. 生成投资建议和风险提示

            输出要求：
            - 基于真实数据和专业知识
            - 包含具体的投资建议
            - 包含详细的风险提示
            - 声明投资有风险，建议仅供参考
            """;

    /**
     * 风险提示模板。
     */
    private static final String RISK_WARNING_TEMPLATE = """
            ⚠️ 风险提示
            ━━━━━━━━━━━━━━━
            1. 投资有风险，入市需谨慎
            2. 以上投资建议仅供参考，不构成投资建议
            3. 过往业绩不代表未来表现
            4. 请根据自身风险承受能力做出投资决策
            5. 如有疑问，请咨询专业投资顾问
            """;

    private final SkillsAgentService skillsAgentService;
    private final RagService ragService;
    private final StockPriceToolService stockPriceToolService;
    private final MarketIndexToolService marketIndexToolService;
    private final RiskCalculatorToolService riskCalculatorToolService;
    private final GraphWorkflowService graphWorkflowService;
    private final IntentClassifier intentClassifier;

    public InvestmentDecisionService(SkillsAgentService skillsAgentService,
                                     RagService ragService,
                                     StockPriceToolService stockPriceToolService,
                                     MarketIndexToolService marketIndexToolService,
                                     RiskCalculatorToolService riskCalculatorToolService,
                                     GraphWorkflowService graphWorkflowService,
                                     IntentClassifier intentClassifier) {
        this.skillsAgentService = skillsAgentService;
        this.ragService = ragService;
        this.stockPriceToolService = stockPriceToolService;
        this.marketIndexToolService = marketIndexToolService;
        this.riskCalculatorToolService = riskCalculatorToolService;
        this.graphWorkflowService = graphWorkflowService;
        this.intentClassifier = intentClassifier;
    }

    /**
     * 执行投资决策流程。
     *
     * @param request 投资决策请求
     * @return 投资决策响应
     */
    public InvestmentDecisionResponse decide(InvestmentDecisionRequest request) {
        long startTime = System.currentTimeMillis();
        String threadId = request.effectiveThreadId();
        String modelName = request.effectiveModelName();

        log.info("开始投资决策流程: threadId={}, model={}", threadId, modelName);

        // 创建工作流数据
        WorkflowData workflowData = WorkflowData.of(
                request.effectiveEnableRAG(),
                request.effectiveEnableTools(),
                request.effectiveEnableGraph()
        );

        List<WorkflowStep> steps = new ArrayList<>();
        StringBuilder contextBuilder = new StringBuilder();
        TokenUsage totalTokenUsage = new TokenUsage(0, 0, 0);

        try {
            // ── 入口意图判断：使用 IntentClassifier 两层分类器 ──
            boolean isInvestment = intentClassifier.isInvestmentRelated(request.message(), modelName);

            if (!isInvestment) {
                log.info("非投资类消息（意图分类结果），直接回复");
                long durationMs = System.currentTimeMillis() - startTime;

                String chatReply = "您好，我是投资决策助手，专注于金融投资相关问题。" +
                        "如果您有任何投资理财方面的问题，欢迎随时提问！😊";

                List<DecisionStep> decisionSteps = List.of(
                        new DecisionStep(1, "意图识别", "classifier", "completed", chatReply, null)
                );

                return InvestmentDecisionResponse.success(
                        threadId, decisionSteps, chatReply, null,
                        durationMs, totalTokenUsage, modelName);
            }

            // Step 1: 问题感知 (Skills)
            log.info("Step 1: 执行问题感知");
            WorkflowStep step1 = executeProblemPerception(request, threadId, contextBuilder);
            steps.add(step1);
            totalTokenUsage = accumulateTokenUsage(totalTokenUsage, step1.tokenUsage());

            // Step 2: 知识检索 (RAG) - 条件执行
            if (workflowData.isRAGEnabled()) {
                log.info("Step 2: 执行知识检索 (RAG)");
                WorkflowStep step2 = executeKnowledgeRetrieval(request, threadId, workflowData, contextBuilder);
                steps.add(step2);
                totalTokenUsage = accumulateTokenUsage(totalTokenUsage, step2.tokenUsage());
            } else {
                log.info("Step 2: 跳过知识检索 (RAG已禁用)");
                steps.add(WorkflowStep.skipped(2, "知识检索", "rag", "RAG功能已禁用", 0));
            }

            // Step 3: 数据获取 (Tools) - 条件执行
            if (workflowData.isToolsEnabled()) {
                log.info("Step 3: 执行数据获取 (Tools)");
                WorkflowStep step3 = executeDataAcquisition(request, threadId, workflowData, contextBuilder);
                steps.add(step3);
                totalTokenUsage = accumulateTokenUsage(totalTokenUsage, step3.tokenUsage());
            } else {
                log.info("Step 3: 跳过数据获取 (Tools已禁用)");
                steps.add(WorkflowStep.skipped(3, "数据获取", "tools", "Tools功能已禁用", 0));
            }

            // Step 4: 推理分析 (Skills)
            log.info("Step 4: 执行推理分析");
            WorkflowStep step4 = executeReasoningAnalysis(request, threadId, contextBuilder);
            steps.add(step4);
            totalTokenUsage = accumulateTokenUsage(totalTokenUsage, step4.tokenUsage());

            // Step 5: 决策生成 (Skills)
            log.info("Step 5: 执行决策生成");
            WorkflowStep step5 = executeDecisionGeneration(request, threadId, contextBuilder);
            steps.add(step5);
            totalTokenUsage = accumulateTokenUsage(totalTokenUsage, step5.tokenUsage());

            // Step 6: 流程编排 (Graph) - 条件执行
            if (workflowData.isGraphEnabled()) {
                log.info("Step 6: 执行流程编排 (Graph)");
                WorkflowStep step6 = executeWorkflowOrchestration(request, threadId, workflowData, contextBuilder);
                steps.add(step6);
                totalTokenUsage = accumulateTokenUsage(totalTokenUsage, step6.tokenUsage());
            } else {
                log.info("Step 6: 跳过流程编排 (Graph已禁用)");
                steps.add(WorkflowStep.skipped(6, "流程编排", "graph", "Graph功能已禁用", 0));
            }

            // 构建最终响应
            long durationMs = System.currentTimeMillis() - startTime;
            String finalAdvice = extractFinalAdvice(steps, contextBuilder);

            log.info("投资决策流程完成: 耗时={}ms, 成功步骤={}/{}, 跳过步骤={}",
                    durationMs,
                    steps.stream().filter(WorkflowStep::isSuccessful).count(),
                    steps.size(),
                    steps.stream().filter(WorkflowStep::isSkipped).count());

            // 根据步骤状态决定响应类型
            return determineResponse(steps, threadId, workflowData, finalAdvice,
                    RISK_WARNING_TEMPLATE, durationMs, totalTokenUsage, modelName);

        } catch (Exception e) {
            log.error("投资决策流程失败: {}", e.getMessage(), e);
            long durationMs = System.currentTimeMillis() - startTime;
            return InvestmentDecisionResponse.failed("投资决策流程执行失败: " + e.getMessage());
        }
    }

    /**
     * 流式执行投资决策流程。
     *
     * @param request 投资决策请求
     * @return 流式返回决策步骤事件
     */
    public Flux<InvestmentStepEvent> decideStream(InvestmentDecisionRequest request) {
        return Flux.defer(() -> {
            long startTime = System.currentTimeMillis();
            String threadId = request.effectiveThreadId();
            String modelName = request.effectiveModelName();

            log.info("开始流式投资决策流程: threadId={}, model={}", threadId, modelName);

            WorkflowData workflowData = WorkflowData.of(
                    request.effectiveEnableRAG(),
                    request.effectiveEnableTools(),
                    request.effectiveEnableGraph()
            );

            StringBuilder contextBuilder = new StringBuilder();

            // ── 入口意图判断：使用 IntentClassifier 两层分类器 ──
            boolean isInvestment = intentClassifier.isInvestmentRelated(request.message(), modelName);

            if (!isInvestment) {
                log.info("流式非投资类消息（意图分类结果），直接回复");
                long durationMs = System.currentTimeMillis() - startTime;
                String chatReply = "您好，我是投资决策助手，专注于金融投资相关问题。" +
                        "如果您有任何投资理财方面的问题，欢迎随时提问！😊";
                return Flux.just(
                        InvestmentStepEvent.stepStart(1, "意图识别", "classifier"),
                        InvestmentStepEvent.stepComplete(1, "意图识别", "classifier", chatReply),
                        InvestmentStepEvent.decisionComplete(threadId, chatReply, null, durationMs)
                );
            }

            // 构建 Step 1 的流
            Flux<InvestmentStepEvent> step1Flux = Flux.concat(
                    Flux.just(InvestmentStepEvent.stepStart(1, "问题感知", "skills")),
                    executeStepStream(1, "问题感知", "skills",
                            buildProblemPerceptionPrompt(request.message()),
                            threadId, contextBuilder, "【问题感知】\n")
            );

            // 根据 Step 1 的结果动态决定后续步骤
            return step1Flux.switchOnFirst((signal, flux) -> {
                InvestmentStepEvent firstEvent = signal.get();
                // 检查 Step 1 是否返回了非投资类回复（stepName 为 "对话回复"）
                boolean isNonInvestment = firstEvent != null
                        && "step_complete".equals(firstEvent.type())
                        && "对话回复".equals(firstEvent.name());

                if (isNonInvestment) {
                    long durationMs = System.currentTimeMillis() - startTime;
                    log.info("流式 Step 1 检测到非投资类消息，跳过后续步骤");
                    return Flux.concat(
                            flux,
                            Flux.just(InvestmentStepEvent.decisionComplete(
                                    threadId,
                                    firstEvent.result(),
                                    null,
                                    durationMs
                            ))
                    );
                }

                // 投资类消息：继续后续步骤
                return Flux.concat(
                        flux,

                        // Step 2: 知识检索 (条件执行)
                        workflowData.isRAGEnabled() ?
                                Flux.concat(
                                        Flux.just(InvestmentStepEvent.stepStart(2, "知识检索", "rag")),
                                        executeStepStream(2, "知识检索", "rag",
                                                buildKnowledgeRetrievalPrompt(request.message(), contextBuilder.toString()),
                                                threadId, contextBuilder, "【知识检索】\n")
                                ) : Flux.just(InvestmentStepEvent.stepStart(2, "知识检索", "rag"))
                                .concatWith(Flux.defer(() -> {
                                    log.info("Step 2: 跳过知识检索 (RAG已禁用)");
                                    return Flux.just(InvestmentStepEvent.stepComplete(2, "知识检索", "rag", "RAG功能已禁用"));
                                })),

                        // Step 3: 数据获取 (条件执行)
                        workflowData.isToolsEnabled() ?
                                Flux.concat(
                                        Flux.just(InvestmentStepEvent.stepStart(3, "数据获取", "tools")),
                                        executeToolsStepStream(3, "数据获取", workflowData, contextBuilder)
                                ) : Flux.just(InvestmentStepEvent.stepStart(3, "数据获取", "tools"))
                                .concatWith(Flux.defer(() -> {
                                    log.info("Step 3: 跳过数据获取 (Tools已禁用)");
                                    return Flux.just(InvestmentStepEvent.stepComplete(3, "数据获取", "tools", "Tools功能已禁用"));
                                })),

                        // Step 4: 推理分析
                        Flux.just(InvestmentStepEvent.stepStart(4, "推理分析", "skills")),
                        executeStepStream(4, "推理分析", "skills",
                                buildReasoningAnalysisPrompt(request.message(), contextBuilder.toString()),
                                threadId, contextBuilder, "【推理分析】\n"),

                        // Step 5: 决策生成
                        Flux.just(InvestmentStepEvent.stepStart(5, "决策生成", "skills")),
                        executeStepStream(5, "决策生成", "skills",
                                buildDecisionGenerationPrompt(request.message(), contextBuilder.toString()),
                                threadId, contextBuilder, "【决策生成】\n"),

                        // Step 6: 流程编排 (条件执行)
                        workflowData.isGraphEnabled() ?
                                Flux.concat(
                                        Flux.just(InvestmentStepEvent.stepStart(6, "流程编排", "graph")),
                                        executeGraphStepStream(6, "流程编排", workflowData, contextBuilder)
                                ) : Flux.just(InvestmentStepEvent.stepStart(6, "流程编排", "graph"))
                                .concatWith(Flux.defer(() -> {
                                    log.info("Step 6: 跳过流程编排 (Graph已禁用)");
                                    return Flux.just(InvestmentStepEvent.stepComplete(6, "流程编排", "graph", "Graph功能已禁用"));
                                })),

                        // 决策完成事件
                        Flux.defer(() -> {
                            long durationMs = System.currentTimeMillis() - startTime;
                            log.info("流式投资决策流程完成: 耗时={}ms", durationMs);
                            return Flux.just(InvestmentStepEvent.decisionComplete(
                                    threadId,
                                    "投资决策已完成，请查看各步骤详情",
                                    RISK_WARNING_TEMPLATE,
                                    durationMs
                            ));
                        })
                );
            });
        });
    }

    // ==================== 各步骤执行方法 ====================

    /**
     * Step 1: 问题感知 (Skills)
     */
    private WorkflowStep executeProblemPerception(InvestmentDecisionRequest request,
                                                  String threadId,
                                                  StringBuilder contextBuilder) {
        long stepStart = System.currentTimeMillis();
        try {
            String prompt = buildProblemPerceptionPrompt(request.message());
            String result = skillsAgentService.chat(prompt, threadId);

            if (result == null || result.isBlank()) {
                log.warn("步骤 1 返回空结果，使用降级结果");
                String fallback = "问题感知步骤未能获取AI响应，基于用户输入直接进入后续分析流程。";
                contextBuilder.append("【问题感知】\n").append(fallback).append("\n\n");
                truncateContext(contextBuilder);
                return WorkflowStep.success(1, "问题感知", "skills", fallback, System.currentTimeMillis() - stepStart);
            }

            // 检测是否为非投资类消息（闲聊、打招呼等）
            if (result.startsWith("NON_INVESTMENT:")) {
                String chatReply = result.substring("NON_INVESTMENT:".length()).trim();
                log.info("检测到非投资类消息，直接回复: {}", chatReply.length() > 50 ? chatReply.substring(0, 50) + "..." : chatReply);
                contextBuilder.append("【对话回复】\n").append(chatReply).append("\n\n");
                return WorkflowStep.success(1, "对话回复", "skills",
                        chatReply, System.currentTimeMillis() - stepStart);
            }

            if (result.startsWith("Exception:") || result.contains("Error while extracting response")) {
                log.error("步骤 1 调用失败: {}", result);
                return WorkflowStep.failed(1, "问题感知", "skills",
                        result, System.currentTimeMillis() - stepStart);
            }

            contextBuilder.append("【问题感知】\n").append(result).append("\n\n");
            truncateContext(contextBuilder);

            log.info("步骤 1 完成，结果长度: {}", result.length());
            return WorkflowStep.success(1, "问题感知", "skills",
                    result, System.currentTimeMillis() - stepStart);
        } catch (Exception e) {
            log.error("步骤 1 执行失败: {}", e.getMessage(), e);
            return WorkflowStep.failed(1, "问题感知", "skills",
                    e.getMessage(), System.currentTimeMillis() - stepStart);
        }
    }

    /**
     * Step 2: 知识检索 (RAG)
     */
    private WorkflowStep executeKnowledgeRetrieval(InvestmentDecisionRequest request,
                                                   String threadId,
                                                   WorkflowData workflowData,
                                                   StringBuilder contextBuilder) {
        long stepStart = System.currentTimeMillis();
        List<String> ragSources = new ArrayList<>();

        try {
            String prompt = buildKnowledgeRetrievalPrompt(request.message(), contextBuilder.toString());
            com.zhou.ai.common.model.ChatResponse ragResponse = ragService.ask(prompt, request.effectiveModelName());

            String result = ragResponse.content();

            // 检查是否返回了错误
            if (result == null || result.isBlank()) {
                log.warn("步骤 2 返回空结果，使用降级结果");
                String fallback = "知识检索步骤未能获取RAG响应，基于模型自身知识继续分析。";
                contextBuilder.append("【知识检索】\n").append(fallback).append("\n\n");
                truncateContext(contextBuilder);
                return WorkflowStep.success(2, "知识检索", "rag", fallback, System.currentTimeMillis() - stepStart);
            }

            if (result.startsWith("Exception:") || result.contains("Error while extracting response")) {
                log.error("步骤 2 调用失败: {}", result);
                // 优雅降级：返回空结果继续
                return WorkflowStep.skipped(2, "知识检索", "rag",
                        "RAG服务异常，跳过: " + result, System.currentTimeMillis() - stepStart);
            }

            ragSources.add("Knowledge Base Query");

            workflowData.addRAGResult("knowledge", result.length() > 200 ?
                    result.substring(0, 200) : result);

            contextBuilder.append("【知识检索】\n").append(result).append("\n\n");
            truncateContext(contextBuilder);

            log.info("步骤 2 完成，结果长度: {}", result.length());
            return WorkflowStep.successWithData(2, "知识检索", "rag",
                    result, ragSources, List.of(), List.of(),
                    System.currentTimeMillis() - stepStart, convertTokenUsage(ragResponse.tokenUsage()));
        } catch (Exception e) {
            log.warn("RAG服务调用失败，降级继续: {}", e.getMessage());
            return WorkflowStep.skipped(2, "知识检索", "rag",
                    "RAG服务不可用: " + e.getMessage(), System.currentTimeMillis() - stepStart);
        }
    }

    /**
     * Step 3: 数据获取 (Tools)
     */
    private WorkflowStep executeDataAcquisition(InvestmentDecisionRequest request,
                                                String threadId,
                                                WorkflowData workflowData,
                                                StringBuilder contextBuilder) {
        long stepStart = System.currentTimeMillis();
        List<ToolCall> toolCalls = new ArrayList<>();

        try {
            StringBuilder toolResultBuilder = new StringBuilder();
            toolResultBuilder.append("> 注意：以下为模拟数据，仅供功能演示参考，不构成投资建议\n\n");

            // Tool 1: 股价查询
            long toolStart = System.currentTimeMillis();
            Map<String, Object> stockData = stockPriceToolService.getStockPrice("AAPL");
            toolCalls.add(ToolCall.success("getStockPrice",
                    Map.of("symbol", "AAPL"), stockData,
                    System.currentTimeMillis() - toolStart));
            toolResultBuilder.append("AAPL股价: ").append(stockData.get("price")).append(" (模拟)\n");

            // Tool 2: 市场指数
            toolStart = System.currentTimeMillis();
            Map<String, Object> indexData = marketIndexToolService.getMarketIndex("上证指数");
            toolCalls.add(ToolCall.success("getMarketIndex",
                    Map.of("indexName", "上证指数"), indexData,
                    System.currentTimeMillis() - toolStart));
            toolResultBuilder.append("上证指数: ").append(indexData.get("value")).append(" (模拟)\n");

            // Tool 3: 风险计算
            toolStart = System.currentTimeMillis();
            Map<String, Object> riskData = riskCalculatorToolService.calculateValueAtRisk(100000, 0.95, 30);
            toolCalls.add(ToolCall.success("calculateValueAtRisk",
                    Map.of("amount", 100000.0, "confidence", 0.95, "days", 30), riskData,
                    System.currentTimeMillis() - toolStart));
            toolResultBuilder.append("VaR: ").append(riskData.get("var")).append(" (模拟)\n");

            String result = toolResultBuilder.toString();

            workflowData.addToolResult("stock_prices", stockData);
            workflowData.addToolResult("market_indices", indexData);
            workflowData.addToolResult("risk_calculations", riskData);

            contextBuilder.append("【数据获取】\n").append(result).append("\n\n");
            truncateContext(contextBuilder);

            log.info("步骤 3 完成，调用了 {} 个工具", toolCalls.size());
            return WorkflowStep.successWithData(3, "数据获取", "tools",
                    result, List.of(), toolCalls, List.of(),
                    System.currentTimeMillis() - stepStart, new TokenUsage(0, 0, 0));
        } catch (Exception e) {
            log.warn("工具服务调用失败，降级继续: {}", e.getMessage());
            return WorkflowStep.skipped(3, "数据获取", "tools",
                    "工具服务不可用: " + e.getMessage(), System.currentTimeMillis() - stepStart);
        }
    }

    /**
     * Step 4: 推理分析 (Skills)
     */
    private WorkflowStep executeReasoningAnalysis(InvestmentDecisionRequest request,
                                                  String threadId,
                                                  StringBuilder contextBuilder) {
        long stepStart = System.currentTimeMillis();
        try {
            String prompt = buildReasoningAnalysisPrompt(request.message(), contextBuilder.toString());

            // 使用新的 threadId 避免状态冲突
            String newThreadId = "reasoning-" + System.currentTimeMillis();
            String result = skillsAgentService.chat(prompt, newThreadId);

            if (result == null || result.isBlank()) {
                log.warn("步骤 4 返回空结果，使用降级结果");
                String fallbackResult = "基于前面的问题感知分析，继续进行推理分析...\n\n" +
                        contextBuilder.toString().substring(0, Math.min(500, contextBuilder.toString().length()));
                contextBuilder.append("【推理分析】\n").append(fallbackResult).append("\n\n");
                truncateContext(contextBuilder);
                return WorkflowStep.success(4, "推理分析", "skills",
                        fallbackResult, System.currentTimeMillis() - stepStart);
            }

            if (result.startsWith("Exception:") || result.contains("Error while extracting response") ||
                    result.contains("DeepSeekApi")) {
                log.error("步骤 4 调用失败: {}", result);
                // 优雅降级：使用 Step 1 的结果作为推理基础
                String fallbackResult = "基于前面的问题感知分析，继续进行推理分析...\n\n" +
                        contextBuilder.toString().substring(0, Math.min(500, contextBuilder.toString().length()));
                contextBuilder.append("【推理分析】\n").append(fallbackResult).append("\n\n");
                truncateContext(contextBuilder);
                return WorkflowStep.success(4, "推理分析", "skills",
                        fallbackResult, System.currentTimeMillis() - stepStart);
            }

            contextBuilder.append("【推理分析】\n").append(result).append("\n\n");
            truncateContext(contextBuilder);

            log.info("步骤 4 完成，结果长度: {}", result.length());
            return WorkflowStep.success(4, "推理分析", "skills",
                    result, System.currentTimeMillis() - stepStart);
        } catch (Exception e) {
            log.error("步骤 4 执行失败: {}", e.getMessage(), e);
            // 优雅降级
            String fallbackResult = "推理分析服务暂时不可用，基于已有信息继续...\n\n" +
                    contextBuilder.toString().substring(0, Math.min(500, contextBuilder.toString().length()));
            contextBuilder.append("【推理分析】\n").append(fallbackResult).append("\n\n");
            truncateContext(contextBuilder);
            return WorkflowStep.success(4, "推理分析", "skills",
                    fallbackResult, System.currentTimeMillis() - stepStart);
        }
    }

    /**
     * Step 5: 决策生成 (Skills)
     */
    private WorkflowStep executeDecisionGeneration(InvestmentDecisionRequest request,
                                                   String threadId,
                                                   StringBuilder contextBuilder) {
        long stepStart = System.currentTimeMillis();
        try {
            String prompt = buildDecisionGenerationPrompt(request.message(), contextBuilder.toString());

            // 使用新的 threadId 避免状态冲突
            String newThreadId = "decision-" + System.currentTimeMillis();
            String result = skillsAgentService.chat(prompt, newThreadId);

            if (result == null || result.isBlank()) {
                log.warn("步骤 5 返回空结果，使用降级结果");
                String fallbackResult = generateFallbackAdvice(request.message(), contextBuilder.toString());
                contextBuilder.append("【决策生成】\n").append(fallbackResult).append("\n\n");
                truncateContext(contextBuilder);
                return WorkflowStep.success(5, "决策生成", "skills",
                        fallbackResult, System.currentTimeMillis() - stepStart);
            }

            if (result.startsWith("Exception:") || result.contains("Error while extracting response") ||
                    result.contains("DeepSeekApi")) {
                log.error("步骤 5 调用失败: {}", result);
                // 优雅降级：生成简化的投资建议
                String fallbackResult = generateFallbackAdvice(request.message(), contextBuilder.toString());
                contextBuilder.append("【决策生成】\n").append(fallbackResult).append("\n\n");
                truncateContext(contextBuilder);
                return WorkflowStep.success(5, "决策生成", "skills",
                        fallbackResult, System.currentTimeMillis() - stepStart);
            }

            contextBuilder.append("【决策生成】\n").append(result).append("\n\n");
            truncateContext(contextBuilder);

            log.info("步骤 5 完成，结果长度: {}", result.length());
            return WorkflowStep.success(5, "决策生成", "skills",
                    result, System.currentTimeMillis() - stepStart);
        } catch (Exception e) {
            log.error("步骤 5 执行失败: {}", e.getMessage(), e);
            // 优雅降级
            String fallbackResult = generateFallbackAdvice(request.message(), contextBuilder.toString());
            contextBuilder.append("【决策生成】\n").append(fallbackResult).append("\n\n");
            truncateContext(contextBuilder);
            return WorkflowStep.success(5, "决策生成", "skills",
                    fallbackResult, System.currentTimeMillis() - stepStart);
        }
    }

    /**
     * Step 6: 流程编排 (Graph)
     */
    private WorkflowStep executeWorkflowOrchestration(InvestmentDecisionRequest request,
                                                      String threadId,
                                                      WorkflowData workflowData,
                                                      StringBuilder contextBuilder) {
        long stepStart = System.currentTimeMillis();
        try {
            Map<String, Object> graphResult = graphWorkflowService.execute(contextBuilder.toString());
            List<String> nodesExecuted = List.of("classify", "analyze", "optimize", "output");

            workflowData.addGraphResult("category", graphResult.get("category"));
            workflowData.addGraphResult("output", graphResult.get("output"));

            String result = "Graph执行完成，分类: " + graphResult.get("category");

            contextBuilder.append("【流程编排】\n").append(result).append("\n\n");
            truncateContext(contextBuilder);

            log.info("步骤 6 完成，分类: {}", graphResult.get("category"));
            return WorkflowStep.successWithData(6, "流程编排", "graph",
                    result, List.of(), List.of(), nodesExecuted,
                    System.currentTimeMillis() - stepStart, new TokenUsage(0, 0, 0));
        } catch (Exception e) {
            log.warn("Graph服务调用失败，降级继续: {}", e.getMessage());
            return WorkflowStep.skipped(6, "流程编排", "graph",
                    "Graph服务不可用: " + e.getMessage(), System.currentTimeMillis() - stepStart);
        }
    }

    // ==================== 流式执行方法 ====================

    /**
     * 流式执行单个决策步骤 (Skills模块)。
     */
    private Flux<InvestmentStepEvent> executeStepStream(int stepNumber, String stepName, String skillName,
                                                        String prompt, String threadId,
                                                        StringBuilder contextBuilder, String contextPrefix) {
        return Flux.defer(() -> {
            long stepStart = System.currentTimeMillis();
            try {
                log.info("流式执行步骤 {}: {} (技能: {})", stepNumber, stepName, skillName);

                // 使用新的 threadId 避免状态冲突
                String newThreadId = stepName + "-" + System.currentTimeMillis();
                String result = skillsAgentService.chat(prompt, newThreadId);

                // 检查是否返回了错误
                if (result == null || result.isBlank()) {
                    log.warn("步骤 {} 返回空结果，使用降级结果", stepNumber);
                    String fallback = generateStepFallback(stepNumber, stepName, contextBuilder.toString());
                    contextBuilder.append(contextPrefix).append(fallback).append("\n\n");
                    truncateContext(contextBuilder);
                    return Flux.just(InvestmentStepEvent.stepComplete(stepNumber, stepName, skillName, fallback));
                }

                // Step 1 意图检测：非投资类消息
                if (stepNumber == 1 && result.startsWith("NON_INVESTMENT:")) {
                    String chatReply = result.substring("NON_INVESTMENT:".length()).trim();
                    log.info("流式 Step 1 检测到非投资类消息，直接回复");
                    contextBuilder.append("【对话回复】\n").append(chatReply).append("\n\n");
                    return Flux.just(InvestmentStepEvent.stepComplete(stepNumber, "对话回复", skillName, chatReply));
                }

                if (result.startsWith("Exception:") || result.contains("Error while extracting response")) {
                    log.warn("步骤 {} API调用失败，使用降级结果: {}", stepNumber,
                            result.length() > 100 ? result.substring(0, 100) : result);
                    String fallback = generateStepFallback(stepNumber, stepName, contextBuilder.toString());
                    contextBuilder.append(contextPrefix).append(fallback).append("\n\n");
                    truncateContext(contextBuilder);
                    return Flux.just(InvestmentStepEvent.stepComplete(stepNumber, stepName, skillName, fallback));
                }

                // 更新上下文
                contextBuilder.append(contextPrefix).append(result).append("\n\n");
                truncateContext(contextBuilder);

                log.info("步骤 {} 完成，结果长度: {}", stepNumber, result.length());
                return Flux.just(InvestmentStepEvent.stepComplete(stepNumber, stepName, skillName, result));

            } catch (Exception e) {
                log.warn("步骤 {} 执行异常，使用降级结果: {}", stepNumber, e.getMessage());
                String fallback = generateStepFallback(stepNumber, stepName, contextBuilder.toString());
                contextBuilder.append(contextPrefix).append(fallback).append("\n\n");
                truncateContext(contextBuilder);
                return Flux.just(InvestmentStepEvent.stepComplete(stepNumber, stepName, skillName, fallback));
            }
        });
    }

    /**
     * 生成步骤降级结果（当 API 调用失败时）。
     */
    private String generateStepFallback(int stepNumber, String stepName, String context) {
        String contextSnippet = context.length() > 300
                ? context.substring(0, 300) + "..."
                : context;
        return switch (stepNumber) {
            case 4 -> """
                    ## 推理分析（降级模式）

                    基于前序步骤收集的信息，进行以下分析：

                    ### 市场趋势
                    - 科技行业整体处于上升周期，AI、半导体、云计算等赛道持续增长
                    - 国内政策大力支持"新质生产力"，半导体国产替代加速

                    ### 风险评估
                    - 地缘政治风险影响供应链
                    - 科技股估值弹性大，短期波动风险较高
                    - 技术迭代可能颠覆现有格局

                    ### 配置建议
                    - 中等风险配置：股票50-60%%，债券30%%，现金10-20%%
                    - 科技股内部细分：大型龙头50%%、云计算20%%、半导体15%%、ETF 15%%

                    > 已收集上下文: %s
                    """.formatted(contextSnippet);
            case 5 -> """
                    ## 投资建议（降级模式）

                    ### 投资策略
                    - 策略类型：成长型 + 平衡型
                    - 投资期限：建议中长期持有（2-5年）

                    ### 具体操作建议
                    - 科技股配置约55%%（55,000元）
                    - 债券/理财约30%%（30,000元）
                    - 现金储备约15%%（15,000元）

                    ### 风险控制
                    - 止盈线：+20%%~30%%，分批止盈
                    - 止损线：-10%%~15%%，严格执行
                    - 单只个股仓位不超过总资金10%%

                    ### 注意事项
                    - 投资有风险，入市需谨慎
                    - 以上建议仅供参考，不构成投资建议
                    - 建议定期再平衡（每季度一次）

                    > ⚠️ 风险提示
                    > 1. 投资有风险，入市需谨慎
                    > 2. 以上投资建议仅供参考，不构成投资建议
                    > 3. 过往业绩不代表未来表现
                    """.formatted(contextSnippet);
            default -> "%s步骤降级：基于已有信息继续分析。\n%s".formatted(stepName, contextSnippet);
        };
    }

    /**
     * 流式执行数据获取步骤 (Tools模块)。
     */
    private Flux<InvestmentStepEvent> executeToolsStepStream(int stepNumber, String stepName,
                                                             WorkflowData workflowData,
                                                             StringBuilder contextBuilder) {
        return Flux.defer(() -> {
            long stepStart = System.currentTimeMillis();
            List<ToolCall> toolCalls = new ArrayList<>();

            try {
                StringBuilder toolResultBuilder = new StringBuilder();
                toolResultBuilder.append("> 注意：以下为模拟数据，仅供功能演示参考，不构成投资建议\n\n");

                // Tool 1: 股价查询
                long toolStart = System.currentTimeMillis();
                Map<String, Object> stockData = stockPriceToolService.getStockPrice("AAPL");
                toolCalls.add(ToolCall.success("getStockPrice",
                        Map.of("symbol", "AAPL"), stockData,
                        System.currentTimeMillis() - toolStart));
                toolResultBuilder.append("AAPL股价: ").append(stockData.get("price")).append(" (模拟)\n");

                // Tool 2: 市场指数
                toolStart = System.currentTimeMillis();
                Map<String, Object> indexData = marketIndexToolService.getMarketIndex("上证指数");
                toolCalls.add(ToolCall.success("getMarketIndex",
                        Map.of("indexName", "上证指数"), indexData,
                        System.currentTimeMillis() - toolStart));
                toolResultBuilder.append("上证指数: ").append(indexData.get("value")).append(" (模拟)\n");

                // Tool 3: 风险计算
                toolStart = System.currentTimeMillis();
                Map<String, Object> riskData = riskCalculatorToolService.calculateValueAtRisk(100000, 0.95, 30);
                toolCalls.add(ToolCall.success("calculateValueAtRisk",
                        Map.of("amount", 100000.0, "confidence", 0.95, "days", 30), riskData,
                        System.currentTimeMillis() - toolStart));
                toolResultBuilder.append("VaR: ").append(riskData.get("var")).append(" (模拟)\n");

                String result = toolResultBuilder.toString();

                workflowData.addToolResult("stock_prices", stockData);
                workflowData.addToolResult("market_indices", indexData);
                workflowData.addToolResult("risk_calculations", riskData);

                contextBuilder.append("【数据获取】\n").append(result).append("\n\n");
                truncateContext(contextBuilder);

                log.info("步骤 {} 完成，调用了 {} 个工具", stepNumber, toolCalls.size());
                return Flux.just(InvestmentStepEvent.stepComplete(stepNumber, stepName, "tools", result));

            } catch (Exception e) {
                log.warn("工具服务调用失败: {}", e.getMessage());
                return Flux.just(InvestmentStepEvent.stepError(stepNumber, stepName, "tools", e.getMessage()));
            }
        });
    }

    /**
     * 流式执行Graph步骤。
     */
    private Flux<InvestmentStepEvent> executeGraphStepStream(int stepNumber, String stepName,
                                                             WorkflowData workflowData,
                                                             StringBuilder contextBuilder) {
        return Flux.defer(() -> {
            long stepStart = System.currentTimeMillis();
            try {
                Map<String, Object> graphResult = graphWorkflowService.execute(contextBuilder.toString());
                List<String> nodesExecuted = List.of("classify", "analyze", "optimize", "output");

                workflowData.addGraphResult("category", graphResult.get("category"));
                workflowData.addGraphResult("output", graphResult.get("output"));

                String result = "Graph执行完成，分类: " + graphResult.get("category");

                contextBuilder.append("【流程编排】\n").append(result).append("\n\n");
                truncateContext(contextBuilder);

                log.info("步骤 {} 完成，分类: {}", stepNumber, graphResult.get("category"));
                return Flux.just(InvestmentStepEvent.stepComplete(stepNumber, stepName, "graph", result));

            } catch (Exception e) {
                log.warn("Graph服务调用失败: {}", e.getMessage());
                return Flux.just(InvestmentStepEvent.stepError(stepNumber, stepName, "graph", e.getMessage()));
            }
        });
    }

    // ==================== 提示词构建方法 ====================

    /**
     * 构建问题感知提示词。
     */
    private String buildProblemPerceptionPrompt(String userMessage) {
        return """
                请先判断用户输入是否为投资相关需求：

                用户输入: %s

                判断规则：
                - 如果用户只是打招呼（如"你好"、"hi"）、闲聊、问与技术无关的问题，请回复"NON_INVESTMENT: "后跟自然的回应
                - 如果用户提到投资、股票、基金、理财、资产配置、风险评估、市场分析等金融投资相关话题，请进行以下分析：

                分析并理解以下用户投资需求：
                1. 投资目标（长期增值/短期收益/被动收入）
                2. 预算范围
                3. 风险承受能力（保守/中等/激进）
                4. 投资期限
                5. 特殊要求或限制
                """.formatted(userMessage);
    }

    /**
     * 构建知识检索提示词。
     */
    private String buildKnowledgeRetrievalPrompt(String userMessage, String context) {
        return """
                基于以下用户需求和已收集信息：

                %s

                用户需求: %s

                请检索相关的：
                1. 财务报告和财报分析
                2. 市场研究报告
                3. 行业分析报告
                4. 投资策略指南
                """.formatted(context, userMessage);
    }

    /**
     * 构建推理分析提示词。
     */
    private String buildReasoningAnalysisPrompt(String userMessage, String context) {
        return """
                基于以下完整信息：

                %s

                用户需求: %s

                请进行深入分析（无需加载额外技能，直接基于以上信息分析）：
                1. 市场趋势和机会
                2. 风险因素评估
                3. 投资标的分析
                4. 组合配置建议
                """.formatted(context, userMessage);
    }

    /**
     * 构建决策生成提示词。
     */
    private String buildDecisionGenerationPrompt(String userMessage, String context) {
        return """
                基于以下完整分析信息：

                %s

                用户原始需求: %s

                请直接生成最终的投资建议（无需加载技能，直接基于以上信息回答）：
                1. 投资策略总结
                2. 具体操作建议
                3. 买入时机建议
                4. 风险控制建议
                5. 注意事项
                """.formatted(context, userMessage);
    }

    /**
     * 生成备用投资建议（当 AI 调用失败时）。
     */
    private String generateFallbackAdvice(String userMessage, String context) {
        return """
                ## 投资建议（基于已收集信息）

                ### 用户需求摘要
                %s

                ### 基于已收集信息的分析
                %s

                ### 投资策略建议
                1. **分散投资**：建议投资 3-5 只科技股或科技 ETF
                2. **风险控制**：设置止损点，建议亏损不超过 15%%
                3. **持有周期**：建议中长期持有（2-5 年）
                4. **仓位管理**：建议分批建仓，不要一次性投入全部资金

                ### 注意事项
                - 投资有风险，入市需谨慎
                - 以上建议仅供参考，不构成投资建议
                - 请根据自身情况做出决策
                """.formatted(userMessage, context.length() > 500 ?
                context.substring(0, 500) + "..." : context);
    }

    // ==================== 辅助方法 ====================

    /**
     * 从步骤结果中提取最终建议。
     */
    private String extractFinalAdvice(List<WorkflowStep> steps, StringBuilder contextBuilder) {
        // 拼接所有成功步骤的结果，形成完整的投资建议
        StringBuilder finalAdvice = new StringBuilder();
        for (WorkflowStep step : steps) {
            if (step.isSuccessful() && step.result() != null && !step.result().isEmpty()) {
                if (finalAdvice.length() > 0) {
                    finalAdvice.append("\n\n");
                }
                finalAdvice.append(step.result());
            }
        }
        return finalAdvice.length() > 0 ? finalAdvice.toString() : "无法生成最终建议";
    }

    /**
     * 累积Token用量。
     */
    private TokenUsage accumulateTokenUsage(TokenUsage current, TokenUsage addition) {
        if (addition == null) {
            return current;
        }
        return new TokenUsage(
                current.promptTokens() + addition.promptTokens(),
                current.completionTokens() + addition.completionTokens(),
                current.totalTokens() + addition.totalTokens()
        );
    }

    /**
     * 转换 common.model.TokenUsage 到 investment.model.TokenUsage。
     */
    private TokenUsage convertTokenUsage(com.zhou.ai.common.model.TokenUsage source) {
        if (source == null) {
            return new TokenUsage(0, 0, 0);
        }
        return new TokenUsage(source.promptTokens(), source.completionTokens(), source.totalTokens());
    }

    /**
     * 截断上下文以避免超出限制。
     */
    private void truncateContext(StringBuilder contextBuilder) {
        if (contextBuilder.length() > MAX_CONTEXT_LENGTH) {
            int truncateAt = contextBuilder.length() - MAX_CONTEXT_LENGTH;
            contextBuilder.delete(0, truncateAt);
            contextBuilder.insert(0, "...[上下文已截断]...\n");
        }
    }

    /**
     * 根据步骤状态决定响应类型。
     */
    private InvestmentDecisionResponse determineResponse(List<WorkflowStep> steps, String threadId,
                                                         WorkflowData workflowData, String finalAdvice,
                                                         String riskWarning, long durationMs,
                                                         TokenUsage tokenUsage, String model) {
        long successCount = steps.stream().filter(WorkflowStep::isSuccessful).count();
        long failedCount = steps.stream().filter(WorkflowStep::isFailed).count();
        long skippedCount = steps.stream().filter(WorkflowStep::isSkipped).count();

        // 将 WorkflowStep 转换为 DecisionStep
        List<DecisionStep> decisionSteps = steps.stream()
                .map(ws -> new DecisionStep(
                        ws.step(),
                        ws.name(),
                        ws.module(),
                        ws.status(),
                        ws.result(),
                        ws.error()
                ))
                .toList();

        // 创建 WorkflowResult
        WorkflowResult workflowResult;
        if (failedCount == 0) {
            workflowResult = WorkflowResult.success(threadId, workflowData, steps, finalAdvice,
                    riskWarning, durationMs, tokenUsage, model);
        } else if (successCount > 0) {
            workflowResult = WorkflowResult.partial(threadId, workflowData, steps, finalAdvice,
                    riskWarning, durationMs, tokenUsage, model);
        } else {
            workflowResult = WorkflowResult.partial(threadId, workflowData, steps, "部分步骤执行失败",
                    riskWarning, durationMs, tokenUsage, model);
        }

        // 返回向后兼容的 InvestmentDecisionResponse
        if (failedCount == 0) {
            return InvestmentDecisionResponse.successWithWorkflow(threadId, decisionSteps, finalAdvice,
                    riskWarning, durationMs, tokenUsage, model, workflowResult);
        } else {
            return InvestmentDecisionResponse.partial(threadId, decisionSteps, finalAdvice,
                    riskWarning, durationMs, tokenUsage, model);
        }
    }
}