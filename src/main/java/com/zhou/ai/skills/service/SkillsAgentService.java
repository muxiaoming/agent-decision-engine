package com.zhou.ai.skills.service;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Skills Agent 服务 — 封装 ReactAgent 调用，支持多轮对话和技能列表查询。
 */
@Service
public class SkillsAgentService {

    private final ReactAgent skillsAgent;
    private final SkillRegistry skillRegistry;

    public SkillsAgentService(ReactAgent skillsAgent, SkillRegistry skillRegistry) {
        this.skillsAgent = skillsAgent;
        this.skillRegistry = skillRegistry;
    }

    public String chat(String message, String threadId) {
        if (threadId == null || threadId.isBlank()) {
            threadId = UUID.randomUUID().toString();
        }
        try {
            RunnableConfig config = RunnableConfig.builder()
                    .threadId(threadId)
                    .build();
            AssistantMessage response = skillsAgent.call(message, config);
            return response.getText();
        } catch (Exception e) {
            throw new RuntimeException("Skills Agent 调用失败: " + e.getMessage(), e);
        }
    }

    public Map<String, String> listSkills() {
        Map<String, String> skills = new LinkedHashMap<>();
        for (SkillMetadata skill : skillRegistry.listAll()) {
            skills.put(skill.getName(), skill.getDescription());
        }
        return skills;
    }

    public Map<String, Object> getDiagnostics() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("registryType", skillRegistry.getRegistryType());
        info.put("skillCount", skillRegistry.size());
        info.put("skills", listSkills());
        info.put("explanation", "Skills 使用渐进式披露：系统提示仅包含技能名称和描述，"
                + "Agent 按需调用 read_skill(name) 加载完整内容");
        return info;
    }
}
