package com.zhou.ai.agent.controller;

import com.zhou.ai.agent.service.MultiAgentInvestService;
import com.zhou.ai.investment.model.InvestmentDecisionRequest;
import com.zhou.ai.investment.model.InvestmentStepEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 多Agent投资决策控制器（可选独立接口）。
 *
 * <p><b>设计说明：</b>
 * 本控制器提供独立的REST接口访问multiAgent Graph工作流流程，
 * 不和原有 {@code InvestmentDecisionController} 的业务接口耦合。
 *
 * <p><b>与原有控制器的区别：</b>
 * <ul>
 *   <li><b>原有 {@code InvestmentDecisionController}</b>：调用 InvestmentDecisionService
 *       （手写6步Flux流程），路径为 /api/investment/decide/stream</li>
 *   <li><b>本控制器</b>：调用 MultiAgentInvestService（7节点多Agent Graph），
 *       路径为 /api/agent/decide/stream 和 /api/agent/decide/batch</li>
 * </ul>
 *
 * <p>两套接口完全独立，可通过配置 {@code ai.graph.workflow.mode} 选择使用的流程，
 * 或同时运行用于对比。
 *
 * @since 2026-06-30
 */
@Tag(name = "多Agent投资决策", description = "基于7节点多Agent StateGraph的流式投资决策（可选增强方案）")
@RestController
@RequestMapping("/agent")
public class MultiAgentController {

    private static final Logger log = LoggerFactory.getLogger(MultiAgentController.class);

    private final MultiAgentInvestService multiAgentInvestService;

    public MultiAgentController(MultiAgentInvestService multiAgentInvestService) {
        this.multiAgentInvestService = multiAgentInvestService;
    }

    /**
     * 流式执行多Agent投资决策流程。
     *
     * @param body 投资决策请求（复用原有InvestmentDecisionRequest结构）
     * @return 流式事件序列，格式与原有 /api/investment/decide/stream 完全一致
     */
    @Operation(
            summary = "流式执行多Agent投资决策",
            description = """
                    使用7节点多Agent StateGraph执行投资决策流程（可选增强方案）：

                    **Agent节点**：
                    1. 意图分类 - 判断是否为投资相关
                    2. 问题感知 - 分析用户需求
                    3. 知识检索 - 向量检索相关金融知识
                    4. 数据获取 - ReAct工具调用获取市场数据
                    5. 推理分析 - 深入分析
                    6. 决策生成 - 生成建议
                    7. 流程编排 - 汇总验证

                    **与原有流程的区别**：
                    - 原有流程使用手写Flux链，本流程使用StateGraph条件路由
                    - 原有流程通过StringBuilder传递上下文，本流程通过Graph State
                    - 事件输出格式完全兼容
                    """
    )
    @RequestMapping(value = "/decide/stream", method = {RequestMethod.GET, RequestMethod.POST},
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<InvestmentStepEvent> decideStream(
            @RequestParam(required = false) String message,
            @RequestParam(required = false, defaultValue = "openAiChatModel") String modelName,
            @RequestParam(required = false) String threadId,
            @RequestBody(required = false) InvestmentDecisionRequest body) {

        String actualMessage = message;
        if (actualMessage == null && body != null) {
            actualMessage = body.message();
        }
        String actualModelName = modelName;
        if (body != null && body.modelName() != null) {
            actualModelName = body.modelName();
        }
        String actualThreadId = threadId;
        if (body != null && body.threadId() != null) {
            actualThreadId = body.threadId();
        }

        log.info("收到multiAgent流式决策请求: message={}", actualMessage);

        if (actualMessage == null || actualMessage.isBlank()) {
            return Flux.just(InvestmentStepEvent.error("message 不能为空"));
        }

        InvestmentDecisionRequest request = new InvestmentDecisionRequest(
                actualMessage, actualModelName, actualThreadId,
                true, true, true
        );

        return multiAgentInvestService.executeStream(request);
    }

    /**
     * 健康检查。
     */
    @Operation(summary = "多Agent服务健康检查")
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "MultiAgentInvestService",
                "description", "7节点多Agent Graph投资决策流程（可选增强方案）",
                "agents", Map.of(
                        "intentClassify", "意图分类",
                        "problemPerception", "问题感知",
                        "knowledgeRetrieval", "知识检索",
                        "dataFetch", "数据获取",
                        "reasoningAnalysis", "推理分析",
                        "decisionGenerate", "决策生成",
                        "graphSchedule", "流程编排"
                ),
                "configuration", Map.of(
                        "ai.graph.workflow.mode", "multi-agent（通过此配置切换流程模式）"
                )
        );
    }
}
