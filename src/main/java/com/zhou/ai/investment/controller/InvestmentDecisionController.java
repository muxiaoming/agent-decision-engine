package com.zhou.ai.investment.controller;

import com.zhou.ai.investment.model.InvestmentDecisionRequest;
import com.zhou.ai.investment.model.InvestmentDecisionResponse;
import com.zhou.ai.investment.model.InvestmentStepEvent;
import com.zhou.ai.investment.service.InvestmentDecisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 投资决策控制器。
 * 提供完整的投资决策流程接口。
 */
@Tag(name = "投资决策", description = "智能投资代理决策引擎 - 完整投资决策流程")
@RestController
@RequestMapping("/investment")
public class InvestmentDecisionController {

    private static final Logger log = LoggerFactory.getLogger(InvestmentDecisionController.class);

    private final InvestmentDecisionService investmentDecisionService;

    public InvestmentDecisionController(InvestmentDecisionService investmentDecisionService) {
        this.investmentDecisionService = investmentDecisionService;
    }

    /**
     * 执行投资决策流程。
     *
     * 该接口串联所有核心功能：
     * 1. 问题感知 (Skills) - 理解需求、选择技能
     * 2. 知识检索 (RAG) - 检索财报、市场报告
     * 3. 数据获取 (Tools) - 查询股价、市场指数、计算风险
     * 4. 推理分析 (Skills) - 基于知识和数据分析
     * 5. 决策生成 (Skills) - 生成投资建议
     * 6. 流程编排 (Graph) - 执行决策流程、验证完整性
     *
     * @param request 投资决策请求
     * @return 投资决策响应，包含所有步骤结果和最终建议
     */
    @Operation(
            summary = "执行投资决策流程",
            description = """
                    一个接口串联完整投资决策流程：

                    **决策步骤**：
                    1. 问题感知 - 分析用户投资需求
                    2. 知识检索 - 检索相关财报和市场报告 (可选)
                    3. 数据获取 - 查询股价、市场指数、计算风险 (可选)
                    4. 推理分析 - 基于知识和数据分析
                    5. 决策生成 - 生成投资建议
                    6. 流程编排 - 执行决策流程、验证完整性 (可选)

                    **功能开关**：
                    - `enableRAG`: 是否启用RAG知识检索 (默认true)
                    - `enableTools`: 是否启用工具调用 (默认true)
                    - `enableGraph`: 是否启用Graph工作流 (默认true)

                    **使用示例**：
                    ```json
                    {
                      "message": "我想投资科技股，预算10万，风险承受能力中等",
                      "modelName": "deepSeekChatModel",
                      "enableRAG": true,
                      "enableTools": true,
                      "enableGraph": true
                    }
                    ```

                    **响应说明**：
                    - 包含所有决策步骤的详细结果
                    - 包含最终投资建议
                    - 包含风险提示
                    - 包含耗时和Token用量
                    - 包含工作流数据（模块使用情况）
                    """
    )
    @RequestMapping(value = "/decide", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<InvestmentDecisionResponse> decide(
            @RequestParam(required = false) String message,
            @RequestParam(required = false, defaultValue = "deepSeekChatModel") String modelName,
            @RequestParam(required = false) String threadId,
            @RequestParam(required = false, defaultValue = "true") boolean enableRAG,
            @RequestParam(required = false, defaultValue = "true") boolean enableTools,
            @RequestParam(required = false, defaultValue = "true") boolean enableGraph,
            @RequestBody(required = false) InvestmentDecisionRequest body) {

        // 优先使用 @RequestParam，如果为空则使用 @RequestBody
        String actualMessage = message;
        if (actualMessage == null && body != null) {
            actualMessage = body.message();
        }

        // 优先使用 @RequestParam，如果为空则使用 @RequestBody 或默认值
        String actualModelName = modelName;
        if (body != null && body.modelName() != null) {
            actualModelName = body.modelName();
        }

        String actualThreadId = threadId;
        if (body != null && body.threadId() != null) {
            actualThreadId = body.threadId();
        }

        boolean actualEnableRAG = enableRAG;
        if (body != null) {
            actualEnableRAG = body.enableRAG();
        }

        boolean actualEnableTools = enableTools;
        if (body != null) {
            actualEnableTools = body.enableTools();
        }

        boolean actualEnableGraph = enableGraph;
        if (body != null) {
            actualEnableGraph = body.enableGraph();
        }

        log.info("收到投资决策请求: message={}, modelName={}, enableRAG={}, enableTools={}, enableGraph={}",
                actualMessage, actualModelName, actualEnableRAG, actualEnableTools, actualEnableGraph);

        if (actualMessage == null || actualMessage.isBlank()) {
            return ResponseEntity.badRequest().body(
                    InvestmentDecisionResponse.failed("message 不能为空")
            );
        }

        // 构建请求对象
        InvestmentDecisionRequest request = new InvestmentDecisionRequest(
                actualMessage,
                actualModelName,
                actualThreadId,
                actualEnableRAG,
                actualEnableTools,
                actualEnableGraph
        );

        InvestmentDecisionResponse response = investmentDecisionService.decide(request);

        log.info("投资决策完成: status={}, durationMs={}",
                response.status(), response.durationMs());

        return ResponseEntity.ok(response);
    }

    /**
     * 流式执行投资决策流程。
     *
     * 该接口以流式方式返回决策步骤，实时显示决策进度。
     *
     * @param request 投资决策请求
     * @return 流式返回决策步骤事件
     */
    @Operation(
            summary = "流式执行投资决策流程",
            description = """
                    以流式方式返回投资决策步骤，实时显示决策进度：

                    **事件类型**：
                    - `step_start`: 步骤开始
                    - `step_complete`: 步骤完成
                    - `step_error`: 步骤失败
                    - `decision_complete`: 决策完成

                    **优势**：
                    - 实时看到决策进度
                    - 及时了解每个步骤的结果
                    - 更好的用户体验
                    """
    )
    @RequestMapping(value = "/decide/stream", method = {RequestMethod.GET, RequestMethod.POST}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<InvestmentStepEvent> decideStream(
            @RequestParam(required = false) String message,
            @RequestParam(required = false, defaultValue = "deepSeekChatModel") String modelName,
            @RequestParam(required = false) String threadId,
            @RequestParam(required = false, defaultValue = "true") boolean enableRAG,
            @RequestParam(required = false, defaultValue = "true") boolean enableTools,
            @RequestParam(required = false, defaultValue = "true") boolean enableGraph,
            @RequestBody(required = false) InvestmentDecisionRequest body) {

        // 优先使用 @RequestParam，如果为空则使用 @RequestBody
        String actualMessage = message;
        if (actualMessage == null && body != null) {
            actualMessage = body.message();
        }

        // 优先使用 @RequestParam，如果为空则使用 @RequestBody 或默认值
        String actualModelName = modelName;
        if (body != null && body.modelName() != null) {
            actualModelName = body.modelName();
        }

        String actualThreadId = threadId;
        if (body != null && body.threadId() != null) {
            actualThreadId = body.threadId();
        }

        boolean actualEnableRAG = enableRAG;
        if (body != null && body.enableRAG() != null) {
            actualEnableRAG = body.enableRAG();
        }

        boolean actualEnableTools = enableTools;
        if (body != null && body.enableTools() != null) {
            actualEnableTools = body.enableTools();
        }

        boolean actualEnableGraph = enableGraph;
        if (body != null && body.enableGraph() != null) {
            actualEnableGraph = body.enableGraph();
        }

        log.info("收到流式投资决策请求: message={}, modelName={}, enableRAG={}, enableTools={}, enableGraph={}",
                actualMessage, actualModelName, actualEnableRAG, actualEnableTools, actualEnableGraph);

        if (actualMessage == null || actualMessage.isBlank()) {
            return Flux.just(InvestmentStepEvent.error("message 不能为空"));
        }

        // 构建请求对象，使用默认值
        InvestmentDecisionRequest request = new InvestmentDecisionRequest(
                actualMessage,
                actualModelName != null ? actualModelName : "deepSeekChatModel",
                actualThreadId,
                actualEnableRAG,
                actualEnableTools,
                actualEnableGraph
        );

        return investmentDecisionService.decideStream(request);
    }

    /**
     * 健康检查。
     *
     * @return 服务状态信息
     */
    @Operation(summary = "投资决策服务健康检查", description = "检查投资决策服务是否正常运行")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "InvestmentDecisionService",
                "description", "智能投资代理决策引擎 - Skills + RAG + Tools + Graph",
                "features", Map.of(
                        "skills", new String[]{
                                "问题感知 (problem-perception)",
                                "推理分析 (reasoning-analysis)",
                                "决策生成 (decision-generation)"
                        },
                        "rag", "知识检索 - 检索财报和市场报告",
                        "tools", new String[]{
                                "股价查询 (getStockPrice)",
                                "市场指数 (getMarketIndex)",
                                "风险计算 (calculateValueAtRisk)"
                        },
                        "graph", "流程编排 - 执行决策流程和验证"
                ),
                "configuration", Map.of(
                        "enableRAG", "是否启用RAG知识检索",
                        "enableTools", "是否启用工具调用",
                        "enableGraph", "是否启用Graph工作流"
                )
        ));
    }

    /**
     * 获取支持的投资场景。
     *
     * @return 支持的投资场景列表
     */
    @Operation(summary = "获取支持的投资场景", description = "列出系统支持的各种投资决策场景")
    @GetMapping("/scenarios")
    public ResponseEntity<Map<String, Object>> scenarios() {
        return ResponseEntity.ok(Map.of(
                "scenarios", new Object[]{
                        Map.of(
                                "name", "股票投资",
                                "description", "分析股票市场，推荐个股",
                                "example", "我想投资科技股，预算10万"
                        ),
                        Map.of(
                                "name", "基金投资",
                                "description", "分析基金市场，推荐基金产品",
                                "example", "推荐一些稳健型基金"
                        ),
                        Map.of(
                                "name", "资产配置",
                                "description", "根据风险偏好优化资产配置",
                                "example", "帮我配置一个平衡型投资组合"
                        ),
                        Map.of(
                                "name", "风险评估",
                                "description", "评估投资组合的风险水平",
                                "example", "评估我的投资组合风险"
                        ),
                        Map.of(
                                "name", "市场分析",
                                "description", "分析市场走势和投资机会",
                                "example", "分析一下A股市场走势"
                        )
                },
                "usage", "POST /api/investment/decide"
        ));
    }
}
