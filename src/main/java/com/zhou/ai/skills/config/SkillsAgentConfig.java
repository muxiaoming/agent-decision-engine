package com.zhou.ai.skills.config;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.classpath.ClasspathSkillRegistry;
import com.zhou.ai.tools.service.CalculatorToolService;
import com.zhou.ai.tools.service.WeatherToolService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Spring AI Alibaba Skills 配置 — 渐进式披露模式。
 * 系统提示仅注入技能摘要，Agent 按需调用 read_skill 加载完整内容。
 */
@Configuration
public class SkillsAgentConfig {

    @Bean
    public SkillRegistry skillRegistry() {
        return ClasspathSkillRegistry.builder()
                .classpathPath("skills")
                .autoLoad(true)
                .build();
    }

    @Bean
    public SkillsAgentHook skillsAgentHook(SkillRegistry skillRegistry) {
        return SkillsAgentHook.builder()
                .skillRegistry(skillRegistry)
                .autoReload(true)
                .build();
    }

    @Bean
    public ReactAgent skillsAgent(
            @Qualifier("deepSeekChatModel") ChatModel chatModel,
            SkillsAgentHook skillsAgentHook,
            WeatherToolService weatherToolService,
            CalculatorToolService calculatorToolService) {

        return ReactAgent.builder()
                .name("skills-agent")
                .model(chatModel)
                .instruction("你是一个智能助手，拥有多种技能（Skills）。"
                        + "根据用户的问题，先判断是否需要加载某个技能，"
                        + "如果需要，使用 read_skill 工具获取完整技能内容后再回答。"
                        + "你也可以使用 queryWeather 和 calculate 工具来辅助回答。")
                .methodTools(weatherToolService, calculatorToolService)
                .hooks(List.of(skillsAgentHook))
                .enableLogging(true)
                .build();
    }
}
