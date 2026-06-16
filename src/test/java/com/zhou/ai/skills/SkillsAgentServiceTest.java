package com.zhou.ai.skills;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.zhou.ai.skills.service.SkillsAgentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SkillsAgentService 纯单元测试。
 * 不依赖 Spring 上下文，使用 Mockito 桩 + StubSkillRegistry 验证服务逻辑。
 */
@ExtendWith(MockitoExtension.class)
class SkillsAgentServiceTest {

    private ReactAgent mockAgent;
    private StubSkillRegistry stubRegistry;
    private SkillsAgentService skillsAgentService;

    @BeforeEach
    void setUp() {
        mockAgent = mock(ReactAgent.class);
        stubRegistry = new StubSkillRegistry();
        skillsAgentService = new SkillsAgentService(mockAgent, stubRegistry);
    }

    // ==================== listSkills ====================

    @Test
    @DisplayName("listSkills - 应返回 3 个已注册技能")
    void listSkillsReturnsAllRegistered() {
        Map<String, String> skills = skillsAgentService.listSkills();

        assertEquals(3, skills.size());
        assertTrue(skills.containsKey("java-spring-expert"));
        assertTrue(skills.containsKey("weather-assistant"));
        assertTrue(skills.containsKey("code-reviewer"));
    }

    @Test
    @DisplayName("listSkills - 技能描述应包含关键字")
    void listSkillsDescriptionsContainKeywords() {
        Map<String, String> skills = skillsAgentService.listSkills();

        assertTrue(skills.get("java-spring-expert").contains("Java"));
        assertTrue(skills.get("weather-assistant").contains("天气"));
        assertTrue(skills.get("code-reviewer").contains("审查"));
    }

    // ==================== getDiagnostics ====================

    @Test
    @DisplayName("getDiagnostics - 应包含注册表类型、技能数量、说明")
    void getDiagnosticsComplete() {
        Map<String, Object> diag = skillsAgentService.getDiagnostics();

        assertEquals("Stub", diag.get("registryType"));
        assertEquals(3, diag.get("skillCount"));
        assertNotNull(diag.get("skills"));
        assertTrue(((String) diag.get("explanation")).contains("渐进式披露"));
    }

    // ==================== chat ====================

    @Test
    @DisplayName("chat - 正常调用应返回 Agent 回复")
    void chatReturnsAgentReply() throws Exception {
        doReturn(new AssistantMessage("这是模拟回复"))
                .when(mockAgent).call(anyString(), any(RunnableConfig.class));

        String reply = skillsAgentService.chat("你好", "test-thread-1");

        assertEquals("这是模拟回复", reply);
        verify(mockAgent).call(eq("你好"), any(RunnableConfig.class));
    }

    @Test
    @DisplayName("chat - 应将 threadId 传递到 RunnableConfig")
    void chatPassesThreadId() throws Exception {
        doReturn(new AssistantMessage("ok"))
                .when(mockAgent).call(anyString(), any(RunnableConfig.class));

        skillsAgentService.chat("你好", "specific-thread-123");

        ArgumentCaptor<RunnableConfig> captor = ArgumentCaptor.forClass(RunnableConfig.class);
        verify(mockAgent).call(eq("你好"), captor.capture());
        assertEquals("specific-thread-123", captor.getValue().threadId().orElse(null));
    }

    @Test
    @DisplayName("chat - threadId 为 null 时应自动生成 UUID")
    void chatGeneratesThreadIdWhenNull() throws Exception {
        doReturn(new AssistantMessage("回复"))
                .when(mockAgent).call(anyString(), any(RunnableConfig.class));

        skillsAgentService.chat("你好", null);

        ArgumentCaptor<RunnableConfig> captor = ArgumentCaptor.forClass(RunnableConfig.class);
        verify(mockAgent).call(eq("你好"), captor.capture());
        String id = captor.getValue().threadId().orElse("");
        assertFalse(id.isBlank());
        assertDoesNotThrow(() -> UUID.fromString(id), "应为合法 UUID");
    }

    @Test
    @DisplayName("chat - threadId 为空白时应自动生成")
    void chatGeneratesThreadIdWhenBlank() throws Exception {
        doReturn(new AssistantMessage("回复"))
                .when(mockAgent).call(anyString(), any(RunnableConfig.class));

        skillsAgentService.chat("你好", "   ");

        ArgumentCaptor<RunnableConfig> captor = ArgumentCaptor.forClass(RunnableConfig.class);
        verify(mockAgent).call(eq("你好"), captor.capture());
        assertFalse(captor.getValue().threadId().orElse("").isBlank());
    }

    @Test
    @DisplayName("chat - 应原样传递用户消息")
    void chatPassesMessageUnchanged() throws Exception {
        doReturn(new AssistantMessage("ok"))
                .when(mockAgent).call(anyString(), any(RunnableConfig.class));

        String msg = "Spring Boot 怎么用条件装配？";
        skillsAgentService.chat(msg, "t-1");

        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockAgent).call(msgCaptor.capture(), any(RunnableConfig.class));
        assertEquals(msg, msgCaptor.getValue());
    }

    @Test
    @DisplayName("chat - Agent 异常应包装为 RuntimeException")
    void chatWrapsAgentException() throws Exception {
        doThrow(new RuntimeException("模拟 AI 不可用"))
                .when(mockAgent).call(anyString(), any(RunnableConfig.class));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> skillsAgentService.chat("测试", "t-1"));

        assertTrue(ex.getMessage().contains("Skills Agent 调用失败"));
        assertTrue(ex.getCause().getMessage().contains("模拟 AI 不可用"));
    }

    // ==================== StubSkillRegistry ====================

    private static class StubSkillRegistry implements SkillRegistry {
        private final List<SkillMetadata> skills = List.of(
                createSkill("java-spring-expert",
                        "Java 和 Spring 技术专家。当用户询问 Java 语法、Spring Boot 配置、Spring AI 用法等问题时使用此技能。"),
                createSkill("weather-assistant",
                        "天气查询助手。当用户询问天气、气温、穿衣建议、出行建议等天气相关问题时使用此技能。"),
                createSkill("code-reviewer",
                        "代码审查专家。当用户提交代码片段请求 review、询问代码质量、要求改进建议时使用此技能。")
        );

        private static SkillMetadata createSkill(String name, String desc) {
            SkillMetadata m = new SkillMetadata();
            m.setName(name);
            m.setDescription(desc);
            return m;
        }

        @Override public Optional<SkillMetadata> get(String name) {
            return skills.stream().filter(s -> s.getName().equals(name)).findFirst();
        }
        @Override public List<SkillMetadata> listAll() { return skills; }
        @Override public boolean contains(String name) { return get(name).isPresent(); }
        @Override public int size() { return skills.size(); }
        @Override public void reload() { }
        @Override public String readSkillContent(String name) { return ""; }
        @Override public String getSkillLoadInstructions() { return "use read_skill"; }
        @Override public String getRegistryType() { return "Stub"; }
        @Override public org.springframework.ai.chat.prompt.SystemPromptTemplate getSystemPromptTemplate() { return null; }
    }
}
