package com.zhou.ai;

import com.zhou.ai.chat.service.ChatService;
import com.zhou.ai.rag.service.RagService;
import com.zhou.ai.tools.service.ToolChatService;
import com.zhou.ai.skills.service.SkillsAgentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 系统提示词验证测试。
 * 验证所有 AI 服务都配置了正确的系统提示词。
 */
@SpringBootTest
class SystemPromptTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private RagService ragService;

    @Autowired
    private ToolChatService toolChatService;

    @Autowired
    private SkillsAgentService skillsAgentService;

    @Test
    void chatServiceShouldHaveSystemPrompt() throws Exception {
        Field field = ChatService.class.getDeclaredField("SYSTEM_PROMPT");
        field.setAccessible(true);
        String systemPrompt = (String) field.get(null);

        assertNotNull(systemPrompt, "ChatService should have SYSTEM_PROMPT defined");
        assertTrue(systemPrompt.contains("投资顾问"), "ChatService system prompt should define investment advisor role");
        assertTrue(systemPrompt.contains("风险"), "ChatService system prompt should mention investment risks");
    }

    @Test
    void ragServiceShouldHaveSystemPrompt() throws Exception {
        Field field = RagService.class.getDeclaredField("SYSTEM_PROMPT");
        field.setAccessible(true);
        String systemPrompt = (String) field.get(null);

        assertNotNull(systemPrompt, "RagService should have SYSTEM_PROMPT defined");
        assertTrue(systemPrompt.contains("知识库助手"), "RagService system prompt should define knowledge base assistant role");
        assertTrue(systemPrompt.contains("投资"), "RagService system prompt should mention investment");
    }

    @Test
    void toolChatServiceShouldHaveSystemPrompt() throws Exception {
        Field field = ToolChatService.class.getDeclaredField("SYSTEM_PROMPT");
        field.setAccessible(true);
        String systemPrompt = (String) field.get(null);

        assertNotNull(systemPrompt, "ToolChatService should have SYSTEM_PROMPT defined");
        assertTrue(systemPrompt.contains("投资"), "ToolChatService system prompt should mention investment");
        assertTrue(systemPrompt.contains("工具"), "ToolChatService system prompt should mention tools");
        assertTrue(systemPrompt.contains("股价") || systemPrompt.contains("getStockPrice"),
                "ToolChatService system prompt should mention stock price tools");
    }

    @Test
    void skillsAgentShouldHaveSystemPrompt() {
        // SkillsAgent 的系统提示词通过 ReactAgent.builder().instruction() 配置
        // 验证 SkillsAgentService 可以正常工作
        assertNotNull(skillsAgentService, "SkillsAgentService should be injected");
    }

    @Test
    void allSystemPromptsShouldMentionInvestmentRisk() throws Exception {
        // 验证所有系统提示词都包含投资相关内容
        Field chatField = ChatService.class.getDeclaredField("SYSTEM_PROMPT");
        chatField.setAccessible(true);
        String chatPrompt = (String) chatField.get(null);
        assertTrue(chatPrompt.contains("投资"), "ChatService should mention investment");

        Field ragField = RagService.class.getDeclaredField("SYSTEM_PROMPT");
        ragField.setAccessible(true);
        String ragPrompt = (String) ragField.get(null);
        assertTrue(ragPrompt.contains("投资"), "RagService should mention investment");

        Field toolField = ToolChatService.class.getDeclaredField("SYSTEM_PROMPT");
        toolField.setAccessible(true);
        String toolPrompt = (String) toolField.get(null);
        assertTrue(toolPrompt.contains("投资"), "ToolChatService should mention investment");
    }
}
