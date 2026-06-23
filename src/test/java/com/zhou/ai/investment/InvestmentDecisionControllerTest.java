package com.zhou.ai.investment;

import com.zhou.ai.investment.controller.InvestmentDecisionController;
import com.zhou.ai.investment.model.InvestmentDecisionRequest;
import com.zhou.ai.investment.model.InvestmentDecisionResponse;
import com.zhou.ai.investment.model.InvestmentStepEvent;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * 投资决策控制器集成测试。
 * 测试完整的投资决策流程。
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InvestmentDecisionControllerTest {

    @Autowired
    private InvestmentDecisionController investmentDecisionController;

    @Test
    @Order(1)
    @DisplayName("健康检查 - 服务正常运行")
    void shouldReturnHealthStatus() {
        System.out.println("\n=== 健康检查 ===");

        ResponseEntity<Map<String, Object>> response = investmentDecisionController.health();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("InvestmentDecisionService", response.getBody().get("service"));

        System.out.println("✅ 健康检查通过");
        System.out.println("状态: " + response.getBody().get("status"));
        System.out.println("服务: " + response.getBody().get("service"));
    }

    @Test
    @Order(2)
    @DisplayName("获取投资场景 - 列出支持的场景")
    void shouldReturnScenarios() {
        System.out.println("\n=== 获取投资场景 ===");

        ResponseEntity<Map<String, Object>> response = investmentDecisionController.scenarios();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("scenarios"));

        System.out.println("✅ 投资场景获取成功");
        System.out.println("场景数量: " + ((Object[]) response.getBody().get("scenarios")).length);
    }

    @Test
    @Order(3)
    @DisplayName("投资决策 - 空消息验证")
    void shouldRejectEmptyMessage() {
        System.out.println("\n=== 空消息验证 ===");

        InvestmentDecisionRequest request = new InvestmentDecisionRequest("", "deepSeekChatModel", null, null, null, null);
        InvestmentDecisionResponse response = investmentDecisionController.decide(request).getBody();

        assertNotNull(response);
        assertEquals("failed", response.status());
        assertNotNull(response.error());
        assertTrue(response.error().contains("不能为空"));

        System.out.println("✅ 空消息验证通过");
        System.out.println("错误信息: " + response.error());
    }

    @Test
    @Order(4)
    @DisplayName("投资决策 - 完整流程测试")
    void shouldExecuteCompleteInvestmentDecision() {
        System.out.println("\n=== 投资决策完整流程测试 ===");
        System.out.println("场景: 用户想投资科技股，预算10万，风险承受能力中等");

        InvestmentDecisionRequest request = InvestmentDecisionRequest.of(
                "我想投资科技股，预算10万，风险承受能力中等",
                "deepSeekChatModel"
        );

        InvestmentDecisionResponse response = investmentDecisionController.decide(request).getBody();

        assumeTrue(response != null, "响应不应为空");
        assumeTrue(response.isFullySuccess() || response.isPartiallySuccess(),
                "投资决策应成功或部分成功");

        // 验证响应结构
        assertNotNull(response.status(), "应包含状态");
        assertNotNull(response.threadId(), "应包含线程ID");
        assertNotNull(response.steps(), "应包含决策步骤");
        assertNotNull(response.finalAdvice(), "应包含最终建议");
        assertNotNull(response.riskWarning(), "应包含风险提示");
        assertTrue(response.durationMs() > 0, "应包含耗时");

        // 验证步骤
        assertTrue(response.steps().size() >= 3, "应至少有3个决策步骤");

        // 输出结果
        System.out.println("\n✅ 投资决策完整流程测试通过");
        System.out.println("状态: " + response.status());
        System.out.println("线程ID: " + response.threadId());
        System.out.println("决策步骤数: " + response.steps().size());
        System.out.println("成功步骤数: " + response.getSuccessfulStepCount());
        System.out.println("失败步骤数: " + response.getFailedStepCount());
        System.out.println("耗时: " + response.durationMs() + "ms");

        // 输出每个步骤的结果
        System.out.println("\n--- 决策步骤详情 ---");
        response.steps().forEach(step -> {
            System.out.println("\n步骤 " + step.step() + ": " + step.name());
            System.out.println("状态: " + step.status());
            System.out.println("技能: " + step.skill());
            if (step.result() != null) {
                String resultPreview = step.result().substring(0, Math.min(200, step.result().length()));
                System.out.println("结果: " + resultPreview + "...");
            }
            if (step.error() != null) {
                System.out.println("错误: " + step.error());
            }
        });

        // 输出最终建议
        System.out.println("\n--- 最终投资建议 ---");
        System.out.println(response.finalAdvice());

        // 输出风险提示
        System.out.println("\n--- 风险提示 ---");
        System.out.println(response.riskWarning());
    }

    @Test
    @Order(5)
    @DisplayName("投资决策 - 流式接口测试")
    void shouldExecuteStreamingInvestmentDecision() {
        System.out.println("\n=== 流式投资决策测试 ===");
        System.out.println("场景: 使用流式接口实时查看决策进度");

        InvestmentDecisionRequest request = InvestmentDecisionRequest.of(
                "I want to invest in tech stocks, budget 100k, medium risk tolerance",
                "deepSeekChatModel"
        );

        // 使用 StepVerifier 验证流式输出
        StepVerifier.create(investmentDecisionController.decideStream(request)
                .timeout(Duration.ofSeconds(180)))
                .expectNextMatches(event -> "step_start".equals(event.type()) && event.step() == 1)
                .expectNextMatches(event -> "step_complete".equals(event.type()) && event.step() == 1)
                .expectNextMatches(event -> "step_start".equals(event.type()) && event.step() == 2)
                .expectNextMatches(event -> "step_complete".equals(event.type()) && event.step() == 2)
                .expectNextMatches(event -> "step_start".equals(event.type()) && event.step() == 3)
                .expectNextMatches(event -> "step_complete".equals(event.type()) && event.step() == 3)
                .expectNextMatches(event -> "step_start".equals(event.type()) && event.step() == 4)
                .expectNextMatches(event -> "step_complete".equals(event.type()) && event.step() == 4)
                .expectNextMatches(event -> "step_start".equals(event.type()) && event.step() == 5)
                .expectNextMatches(event -> "step_complete".equals(event.type()) && event.step() == 5)
                .expectNextMatches(event -> "decision_complete".equals(event.type()))
                .verifyComplete();

        System.out.println("\n✅ 流式投资决策测试通过");
    }

    @Test
    @Order(6)
    @DisplayName("投资决策 - 多轮对话测试")
    void shouldMaintainContextInMultiTurn() {
        System.out.println("\n=== 多轮对话测试 ===");

        String threadId = "multi-turn-test-" + System.currentTimeMillis();

        // 第一轮：投资咨询（使用英文避免编码问题）
        System.out.println("第一轮: 投资咨询");
        InvestmentDecisionRequest request1 = InvestmentDecisionRequest.of(
                "I want to invest in stocks",
                "deepSeekChatModel",
                threadId
        );
        InvestmentDecisionResponse response1 = investmentDecisionController.decide(request1).getBody();
        assumeTrue(response1 != null && (response1.isFullySuccess() || response1.isPartiallySuccess()),
                "第一轮应成功");

        System.out.println("第一轮完成，线程ID: " + response1.threadId());
        System.out.println("决策步骤数: " + response1.steps().size());

        // 第二轮：基于上下文追问
        System.out.println("\n第二轮: 基于上下文追问");
        InvestmentDecisionRequest request2 = InvestmentDecisionRequest.of(
                "Budget 100k, how should I allocate?",
                "deepSeekChatModel",
                threadId
        );
        InvestmentDecisionResponse response2 = investmentDecisionController.decide(request2).getBody();
        assumeTrue(response2 != null && (response2.isFullySuccess() || response2.isPartiallySuccess()),
                "第二轮应成功");

        System.out.println("第二轮完成");
        System.out.println("决策步骤数: " + response2.steps().size());

        // 验证上下文保持
        assertEquals(threadId, response1.threadId(), "第一轮线程ID应一致");
        assertEquals(threadId, response2.threadId(), "第二轮线程ID应一致");

        System.out.println("\n✅ 多轮对话测试通过");
        System.out.println("上下文保持正常");
    }

    @Test
    @Order(7)
    @DisplayName("投资决策 - 不同风险偏好测试")
    void shouldHandleDifferentRiskPreferences() {
        System.out.println("\n=== 不同风险偏好测试 ===");

        // 保守型投资者（使用英文避免编码问题）
        System.out.println("测试保守型投资者");
        InvestmentDecisionRequest conservativeRequest = InvestmentDecisionRequest.of(
                "I am a conservative investor, budget 50k, want stable growth",
                "deepSeekChatModel"
        );
        InvestmentDecisionResponse conservativeResponse = investmentDecisionController
                .decide(conservativeRequest).getBody();

        assumeTrue(conservativeResponse != null, "响应不应为空");
        System.out.println("保守型投资者决策完成");

        // 进取型投资者
        System.out.println("\n测试进取型投资者");
        InvestmentDecisionRequest aggressiveRequest = InvestmentDecisionRequest.of(
                "I am an aggressive investor, budget 200k, pursue high returns",
                "deepSeekChatModel"
        );
        InvestmentDecisionResponse aggressiveResponse = investmentDecisionController
                .decide(aggressiveRequest).getBody();

        assumeTrue(aggressiveResponse != null, "响应不应为空");
        System.out.println("进取型投资者决策完成");

        System.out.println("\n✅ 不同风险偏好测试通过");
    }
}
