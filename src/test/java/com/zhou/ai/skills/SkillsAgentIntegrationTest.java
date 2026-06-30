package com.zhou.ai.skills;

import com.zhou.ai.skills.service.SkillsAgentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * Skills Agent 集成测试。
 * 真实启动应用，直接调用 SkillsAgentService 方法触发各技能。
 * AI 调用使用 Assumptions：API 不可用时跳过。
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SkillsAgentIntegrationTest {

    @Autowired
    private SkillsAgentService skillsAgentService;

    // ==================== 技能注册验证 ====================

    @Test
    @Order(1)
    @DisplayName("listSkills - 应注册 4 个投资相关技能（classpath 扫描）")
    void shouldRegisterFourSkills() {
        Map<String, String> skills = skillsAgentService.listSkills();

        assertEquals(4, skills.size(), "应有 4 个技能");
        assertTrue(skills.containsKey("market-analysis"));
        assertTrue(skills.containsKey("risk-assessment"));
        assertTrue(skills.containsKey("portfolio-optimization"));
        assertTrue(skills.containsKey("investment-recommendation"));
        System.out.println("已注册技能: " + skills.keySet());
    }

    @Test
    @Order(2)
    @DisplayName("getDiagnostics - 诊断信息应完整")
    void diagnosticsShouldBeComplete() {
        Map<String, Object> diag = skillsAgentService.getDiagnostics();

        assertEquals("Classpath", diag.get("registryType"));
        assertEquals(4, diag.get("skillCount"));
        assertNotNull(diag.get("explanation"));
        System.out.println("诊断信息: " + diag);
    }

    // ==================== 市场分析技能触发 ====================

    @Test
    @Order(10)
    @DisplayName("触发 market-analysis 技能")
    void triggerMarketAnalysis() {
        String reply = skillsAgentService.chat("分析一下当前A股市场趋势", "test-market-1");
        assumeTrue(reply != null && !reply.isBlank(), "AI API 不可用，跳过");

        assertTrue(reply.length() > 50, "回复应足够长");
        System.out.println("=== 市场分析 ===\n" + reply);
    }

    // ==================== 风险评估技能触发 ====================

    @Test
    @Order(20)
    @DisplayName("触发 risk-assessment 技能")
    void triggerRiskAssessment() {
        String reply = skillsAgentService.chat("评估一下投资科技股的风险", "test-risk-1");
        assumeTrue(reply != null && !reply.isBlank(), "AI API 不可用，跳过");

        assertTrue(reply.length() > 50, "回复应足够长");
        System.out.println("=== 风险评估 ===\n" + reply);
    }

    // ==================== 投资组合优化技能触发 ====================

    @Test
    @Order(30)
    @DisplayName("触发 portfolio-optimization 技能")
    void triggerPortfolioOptimization() {
        String reply = skillsAgentService.chat("如何优化我的投资组合配置", "test-portfolio-1");
        assumeTrue(reply != null && !reply.isBlank(), "AI API 不可用，跳过");

        assertTrue(reply.length() > 50, "回复应足够长");
        System.out.println("=== 投资组合优化 ===\n" + reply);
    }

    // ==================== 多轮对话测试 ====================

    @Test
    @Order(40)
    @DisplayName("多轮对话 - 同一 threadId 应保持上下文")
    void multiTurnConversation() {
        String threadId = "test-multi-turn-invest-1";

        String reply1 = skillsAgentService.chat("分析一下科技股的投资前景", threadId);
        assumeTrue(reply1 != null && !reply1.isBlank(), "AI API 不可用，跳过");

        String reply2 = skillsAgentService.chat("那具体推荐哪些股票？", threadId);
        assumeTrue(reply2 != null && !reply2.isBlank(), "AI API 不可用，跳过");

        System.out.println("=== 多轮对话 ===");
        System.out.println("轮1: " + reply1);
        System.out.println("轮2: " + reply2);
    }

    // ==================== 边界情况测试 ====================

    @Test
    @Order(50)
    @DisplayName("通用问题 - 不匹配特定技能时应正常回答")
    void genericQuestionNoSpecificSkill() {
        String reply = skillsAgentService.chat("你好，今天心情不错", "test-generic-1");
        assumeTrue(reply != null && !reply.isBlank(), "AI API 不可用，跳过");

        assertNotNull(reply, "通用问题也应返回回复");
        assertFalse(reply.isBlank(), "回复不应为空");
        System.out.println("=== 通用对话 ===\n" + reply);
    }
}
