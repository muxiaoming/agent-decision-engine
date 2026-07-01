package com.zhou.ai.agent.config;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.zhou.ai.agent.tool.KnowledgeRetrievalTool;
import com.zhou.ai.tools.service.MarketIndexToolService;
import com.zhou.ai.tools.service.RiskCalculatorToolService;
import com.zhou.ai.tools.service.StockPriceToolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ReactAgent 工厂 — 按模型名称缓存并分发 ReactAgent 实例。
 *
 * <p><b>设计说明：</b>
 * ReactAgent 的模型（ChatModel）在 {@code .model(chatModel)} 时固定，
 * 但前端传参允许动态切换模型。本工厂为每个 Agent 类型 + 模型名称
 * 维护一个缓存，首次请求时构建并编译，后续复用。
 *
 * <p><b>线程安全：</b>使用 {@link ConcurrentHashMap#computeIfAbsent}，
 * 确保每个模型+Agent组合仅编译一次。
 *
 * @since 2026-07-01
 */
@Component
public class ReactAgentFactory {

    private static final Logger log = LoggerFactory.getLogger(ReactAgentFactory.class);

    private static final String DEFAULT_MODEL = "openAiChatModel";

    // ==================== 系统提示词（与原有 MultiChatClientConfig 保持一致） ====================

    private static final String PROBLEM_PERCEPTION_PROMPT = """
            你是一个投资需求分析师。你的职责是分析用户输入，提取投资需求关键信息。

            按以下步骤：
            1. 识别投资目标（长期增值/短期收益/被动收入）
            2. 提取预算范围
            3. 判断风险承受能力（保守/中等/激进）
            4. 明确投资期限
            5. 输出结构化分析结果

            只做分析，不做推荐。信息不足时标注"信息不足"。
            """;

    private static final String KNOWLEDGE_RETRIEVAL_PROMPT = """
            你是一个金融知识专家。使用 retrieveKnowledge 工具从知识库检索相关金融文档。

            操作流程：
            1. 理解用户需求（用户输入中会包含需求和前置分析结果）
            2. 调用 retrieveKnowledge 工具检索
            3. 整理检索结果，提取关键信息
            4. 输出结构化知识摘要

            如果检索无结果，基于自身金融知识提供分析，标注"基于通用知识"。
            如果上游未提供数据，会标注"（上游未提供数据）"或"（未提供）"，请照常处理。
            """;

    private static final String DATA_FETCH_PROMPT = """
            你是一个金融市场数据专员，可使用以下工具获取实时数据：

            可用工具：
            - getStockPrice(symbol)：查询个股实时价格
            - getMarketIndex(indexName)：查询大盘指数
            - getMarketVolatility()：查询波动率
            - getMarketSentiment()：查询市场情绪
            - getSectorPerformance(sector)：查询行业板块表现
            - calculateValueAtRisk(amount, confidence, days)：计算VaR
            - calculateSharpeRatio(return, riskFree, volatility)：计算夏普比率
            - calculatePortfolioReturn(stock, bond, cash)：计算组合收益

            根据用户需求自主选择并调用适当工具获取数据，然后汇总输出结构化的数据摘要。
            用户输入中会包含需求说明和前置Agent的分析结果。
            """;

    private static final String REASONING_ANALYSIS_PROMPT = """
            你是一个资深金融分析师。基于用户提供的完整数据进行投资分析。

            分析步骤：
            1. 回顾已有数据（用户需求、知识检索、市场数据）
            2. 分析市场趋势和投资机会
            3. 评估风险因素和潜在回报
            4. 给出初步配置建议
            5. 标注结论置信度

            严格依赖数据，引用具体来源。标注不确定性等级。
            """;

    private static final String DECISION_GENERATE_PROMPT = """
            你是一个资深投资顾问。基于前序Agent的推理分析结果生成投资建议。

            输出包含：
            1. 投资策略总结
            2. 具体操作建议（标的、比例、时机）
            3. 风险控制措施（止盈止损、仓位管理）
            4. 投资期限建议
            5. 注意事项

            严格基于数据，标注置信度，包含风险声明。
            """;

    private static final String GRAPH_SCHEDULE_PROMPT = """
            你是一个投资决策流程的管理者。汇总所有Agent执行结果，生成最终决策报告。

            输出：
            1. 流程执行总结
            2. 信息一致性评估
            3. 整体决策置信度评级
            4. 下一步建议（如有）
            
            严禁如下:
            1. 免责声明中**禁止出现任何大模型、AI产品具体名称**（禁止出现Agnes、Flash、Sapiens、GPT、通义千问等任何模型/厂商名称）；
            2. 仅能模糊描述为「本报告由AI生成」，不允许标注任何AI版本、模型代号、产品名称；

            用户输入中会包含各Agent的执行结果。
            """;

    // ==================== 依赖注入 ====================

    private final Map<String, ChatModel> chatModels;
    private final StockPriceToolService stockPriceToolService;
    private final MarketIndexToolService marketIndexToolService;
    private final RiskCalculatorToolService riskCalculatorToolService;
    private final KnowledgeRetrievalTool knowledgeRetrievalTool;

    // ==================== 缓存 ====================

    private final ConcurrentHashMap<String, ReactAgent> problemPerceptionCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReactAgent> knowledgeRetrievalCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReactAgent> dataFetchCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReactAgent> reasoningAnalysisCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReactAgent> decisionGenerateCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReactAgent> graphScheduleCache = new ConcurrentHashMap<>();

    public ReactAgentFactory(Map<String, ChatModel> chatModels,
                             StockPriceToolService stockPriceToolService,
                             MarketIndexToolService marketIndexToolService,
                             RiskCalculatorToolService riskCalculatorToolService,
                             KnowledgeRetrievalTool knowledgeRetrievalTool) {
        this.chatModels = chatModels;
        this.stockPriceToolService = stockPriceToolService;
        this.marketIndexToolService = marketIndexToolService;
        this.riskCalculatorToolService = riskCalculatorToolService;
        this.knowledgeRetrievalTool = knowledgeRetrievalTool;
        log.info("ReactAgentFactory 初始化，可用模型: {}, 工具: 知识检索/股价/市场指数/风险计算", chatModels.keySet());
    }

    // ==================== 公开 API ====================

    public ReactAgent getProblemPerceptionAgent(String modelName) {
        return problemPerceptionCache.computeIfAbsent(modelName, this::buildProblemPerceptionAgent);
    }

    public ReactAgent getKnowledgeRetrievalAgent(String modelName) {
        return knowledgeRetrievalCache.computeIfAbsent(modelName, this::buildKnowledgeRetrievalAgent);
    }

    public ReactAgent getDataFetchAgent(String modelName) {
        return dataFetchCache.computeIfAbsent(modelName, this::buildDataFetchAgent);
    }

    public ReactAgent getReasoningAnalysisAgent(String modelName) {
        return reasoningAnalysisCache.computeIfAbsent(modelName, this::buildReasoningAnalysisAgent);
    }

    public ReactAgent getDecisionGenerateAgent(String modelName) {
        return decisionGenerateCache.computeIfAbsent(modelName, this::buildDecisionGenerateAgent);
    }

    public ReactAgent getGraphScheduleAgent(String modelName) {
        return graphScheduleCache.computeIfAbsent(modelName, this::buildGraphScheduleAgent);
    }

    // ==================== 构建方法 ====================

    private ReactAgent buildProblemPerceptionAgent(String modelName) {
        ChatModel model = resolveModel(modelName);
        log.info("构建问题感知Agent | model={}", modelName);
        return ReactAgent.builder()
                .name("problem-perception")
                .model(model)
                .compileConfig(CompileConfig.builder().recursionLimit(2).build())
                .instruction(PROBLEM_PERCEPTION_PROMPT)
                .includeContents(true)
                .build();
    }

    private ReactAgent buildKnowledgeRetrievalAgent(String modelName) {
        ChatModel model = resolveModel(modelName);
        log.info("构建知识检索Agent | model={}", modelName);
        return ReactAgent.builder()
                .name("knowledge-retrieval")
                .model(model)
                .compileConfig(CompileConfig.builder().recursionLimit(1).build())
                .instruction(KNOWLEDGE_RETRIEVAL_PROMPT)
                .methodTools(knowledgeRetrievalTool)
                .includeContents(true)
                .build();
    }

    private ReactAgent buildDataFetchAgent(String modelName) {
        ChatModel model = resolveModel(modelName);
        log.info("构建数据获取Agent | model={}", modelName);
        return ReactAgent.builder()
                .name("data-fetch")
                .model(model)
                .compileConfig(CompileConfig.builder().recursionLimit(2).build())
                .instruction(DATA_FETCH_PROMPT)
                .methodTools(stockPriceToolService, marketIndexToolService, riskCalculatorToolService)
                .includeContents(true)
                .build();
    }

    private ReactAgent buildReasoningAnalysisAgent(String modelName) {
        ChatModel model = resolveModel(modelName);
        log.info("构建推理分析Agent | model={}", modelName);
        return ReactAgent.builder()
                .name("reasoning-analysis")
                .model(model)
                .compileConfig(CompileConfig.builder().recursionLimit(3).build())
                .instruction(REASONING_ANALYSIS_PROMPT)
                .includeContents(true)
                .build();
    }

    private ReactAgent buildDecisionGenerateAgent(String modelName) {
        ChatModel model = resolveModel(modelName);
        log.info("构建决策生成Agent | model={}", modelName);
        return ReactAgent.builder()
                .name("decision-generate")
                .model(model)
                .compileConfig(CompileConfig.builder().recursionLimit(3).build())
                .instruction(DECISION_GENERATE_PROMPT)
                .includeContents(true)
                .build();
    }

    private ReactAgent buildGraphScheduleAgent(String modelName) {
        ChatModel model = resolveModel(modelName);
        log.info("构建流程编排Agent | model={}", modelName);
        return ReactAgent.builder()
                .name("graph-schedule")
                .model(model)
                .compileConfig(CompileConfig.builder().recursionLimit(3).build())
                .instruction(GRAPH_SCHEDULE_PROMPT)
                .includeContents(true)
                .build();
    }

    // ==================== 辅助方法 ====================

    private ChatModel resolveModel(String modelName) {
        ChatModel model = chatModels.get(modelName);
        if (model == null) {
            log.warn("模型 '{}' 不可用，回退到 '{}'", modelName, DEFAULT_MODEL);
            model = chatModels.get(DEFAULT_MODEL);
        }
        return model;
    }
}
