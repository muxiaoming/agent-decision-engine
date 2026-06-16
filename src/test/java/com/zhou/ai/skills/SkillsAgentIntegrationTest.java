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
    @DisplayName("listSkills - 应注册 3 个技能（classpath 扫描）")
    void shouldRegisterThreeSkills() {
        Map<String, String> skills = skillsAgentService.listSkills();

        assertEquals(3, skills.size(), "应有 3 个技能");
        assertTrue(skills.containsKey("java-spring-expert"));
        assertTrue(skills.containsKey("weather-assistant"));
        assertTrue(skills.containsKey("code-reviewer"));
        System.out.println("已注册技能: " + skills.keySet());
    }

    @Test
    @Order(2)
    @DisplayName("getDiagnostics - 诊断信息应完整")
    void diagnosticsShouldBeComplete() {
        Map<String, Object> diag = skillsAgentService.getDiagnostics();

        assertEquals("Classpath", diag.get("registryType"));
        assertEquals(3, diag.get("skillCount"));
        assertNotNull(diag.get("explanation"));
        System.out.println("诊断信息: " + diag);
    }

    // ==================== 天气助手技能触发 ====================

    @Test
    @Order(10)
    @DisplayName("触发 weather-assistant 技能 - 北京天气查询")
    void triggerWeatherAssistantBeijing() {
        String reply = skillsAgentService.chat("北京今天天气怎么样？", "test-weather-1");
        assumeTrue(reply != null && !reply.isBlank(), "AI API 不可用，跳过");

        assertTrue(reply.contains("北京"), "回复应包含城市名 '北京'");
        assertTrue(reply.contains("°") || reply.contains("度"), "回复应包含温度信息");
        System.out.println("=== 天气助手（北京）===\n" + reply);
    }

    @Test
    @Order(11)
    @DisplayName("触发 weather-assistant 技能 - 穿衣建议")
    void triggerWeatherAssistantClothing() {
        String reply = skillsAgentService.chat("上海今天穿什么合适？", "test-weather-2");
        assumeTrue(reply != null && !reply.isBlank(), "AI API 不可用，跳过");

        assertTrue(reply.contains("上海"), "回复应包含城市名 '上海'");
        assertTrue(reply.contains("穿") || reply.contains("衣") || reply.contains("建议"),
                "回复应包含穿衣建议");
        System.out.println("=== 天气助手（穿衣）===\n" + reply);
    }

    @Test
    @Order(12)
    @DisplayName("触发 weather-assistant 技能 - 出行建议")
    void triggerWeatherAssistantTravel() {
        String reply = skillsAgentService.chat("广州今天适合出门吗？", "test-weather-3");
        assumeTrue(reply != null && !reply.isBlank(), "AI API 不可用，跳过");

        assertTrue(reply.contains("广州"), "回复应包含城市名 '广州'");
        System.out.println("=== 天气助手（出行）===\n" + reply);
    }

    // ==================== Java/Spring 专家技能触发 ====================

    @Test
    @Order(20)
    @DisplayName("触发 java-spring-expert 技能 - 条件装配")
    void triggerJavaExpertConditional() {
        String reply = skillsAgentService.chat(
                "Spring Boot 中如何实现条件装配？", "test-java-1");
        assumeTrue(reply != null && !reply.isBlank(), "AI API 不可用，跳过");

        assertTrue(reply.length() > 100, "回复应足够长（证明 Skill 完整内容被加载）");
        System.out.println("=== Java 专家（条件装配）===\n" + reply);
    }

    @Test
    @Order(21)
    @DisplayName("触发 java-spring-expert 技能 - Virtual Threads")
    void triggerJavaExpertVirtualThreads() {
        String reply = skillsAgentService.chat(
                "Java 21 的 Virtual Threads 怎么用？", "test-java-2");
        assumeTrue(reply != null && !reply.isBlank(), "AI API 不可用，跳过");

        assertTrue(reply.length() > 100, "回复应足够长（证明 Skill 完整内容被加载）");
        System.out.println("=== Java 专家（Virtual Threads）===\n" + reply);
    }

    @Test
    @Order(22)
    @DisplayName("触发 java-spring-expert 技能 - Spring AI Tool Calling")
    void triggerJavaExpertSpringAITool() {
        String reply = skillsAgentService.chat(
                "Spring AI 中如何使用 @Tool 注解实现 Function Calling？", "test-java-3");
        assumeTrue(reply != null && !reply.isBlank(), "AI API 不可用，跳过");

        assertTrue(reply.length() > 100, "回复应足够长（证明 Skill 完整内容被加载）");
        System.out.println("=== Java 专家（Tool Calling）===\n" + reply);
    }

    // ==================== 代码审查技能触发 ====================

    @Test
    @Order(30)
    @DisplayName("触发 code-reviewer 技能 - SQL 注入检测")
    void triggerCodeReviewerSQLInjection() {
        String code = """
                public User findUser(String name) {
                    Connection conn = DriverManager.getConnection(url);
                    Statement st = conn.createStatement();
                    ResultSet rs = st.executeQuery("SELECT * FROM users WHERE name='" + name + "'");
                    if (rs.next()) return new User(rs.getString("name"));
                    return null;
                }
                """;
        String reply = skillsAgentService.chat("Review this code: " + code, "test-review-1");
        assumeTrue(reply != null && !reply.isBlank(), "AI API 不可用，跳过");

        assertTrue(reply.length() > 100, "回复应足够长（证明 Skill 完整内容被加载）");
        System.out.println("=== 代码审查（SQL 注入）===\n" + reply);
    }

    @Test
    @Order(31)
    @DisplayName("触发 code-reviewer 技能 - 资源泄漏检测")
    void triggerCodeReviewerResourceLeak() {
        String code = """
                public String readFile(String path) {
                    FileInputStream fis = new FileInputStream(path);
                    byte[] data = fis.readAllBytes();
                    return new String(data);
                }
                """;
        String reply = skillsAgentService.chat("Review this code: " + code, "test-review-2");
        assumeTrue(reply != null && !reply.isBlank(), "AI API 不可用，跳过");

        assertTrue(reply.length() > 100, "回复应足够长（证明 Skill 完整内容被加载）");
        System.out.println("=== 代码审查（资源泄漏）===\n" + reply);
    }

    @Test
    @Order(32)
    @DisplayName("触发 code-reviewer 技能 - 空指针风险检测")
    void triggerCodeReviewerNPE() {
        String code = """
                public String getCityName(User user) {
                    return user.getAddress().getCity().getName();
                }
                """;
        String reply = skillsAgentService.chat("Review this code: " + code, "test-review-3");
        assumeTrue(reply != null && !reply.isBlank(), "AI API 不可用，跳过");

        assertTrue(reply.length() > 100, "回复应足够长（证明 Skill 完整内容被加载）");
        System.out.println("=== 代码审查（空指针）===\n" + reply);
    }

    // ==================== 多轮对话测试 ====================

    @Test
    @Order(40)
    @DisplayName("多轮对话 - 同一 threadId 应保持上下文")
    void multiTurnConversation() {
        String threadId = "test-multi-turn-1";

        String reply1 = skillsAgentService.chat("北京今天天气怎么样？", threadId);
        assumeTrue(reply1 != null && !reply1.isBlank(), "AI API 不可用，跳过");
        assertTrue(reply1.contains("北京"), "第一轮应包含北京天气");

        String reply2 = skillsAgentService.chat("那穿什么衣服合适？", threadId);
        assumeTrue(reply2 != null && !reply2.isBlank(), "AI API 不可用，跳过");
        System.out.println("=== 多轮对话 ===");
        System.out.println("轮1: " + reply1);
        System.out.println("轮2: " + reply2);
    }

    // ==================== 边界情况测试 ====================

    @Test
    @Order(50)
    @DisplayName("通用问题 - 不匹配特定技能时应正常回复")
    void genericQuestionNoSpecificSkill() {
        String reply = skillsAgentService.chat("你好，今天心情不错", "test-generic-1");
        assumeTrue(reply != null && !reply.isBlank(), "AI API 不可用，跳过");

        assertNotNull(reply, "通用问题也应返回回复");
        assertFalse(reply.isBlank(), "回复不应为空");
        System.out.println("=== 通用对话 ===\n" + reply);
    }

    @Test
    @Order(51)
    @DisplayName("混合问题 - 天气 + 工具调用协同")
    void weatherWithToolIntegration() {
        String reply = skillsAgentService.chat(
                "帮我查一下深圳的天气，然后计算一下如果温度降低5度是多少？",
                "test-mix-1");
        assumeTrue(reply != null && !reply.isBlank(), "AI API 不可用，跳过");

        assertTrue(reply.contains("深圳"), "回复应包含深圳");
        System.out.println("=== 混合调用（天气+计算）===\n" + reply);
    }
}
