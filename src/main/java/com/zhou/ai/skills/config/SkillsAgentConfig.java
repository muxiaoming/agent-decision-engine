package com.zhou.ai.skills.config;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.classpath.ClasspathSkillRegistry;
import com.zhou.ai.tools.service.CalculatorToolService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Spring AI Alibaba Skills 配置 - 渐进式披露模式。
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

    /**
     * Agent 最大推理轮数。
     * 每轮 = 一次 LLM 调用 + tool 执行。超过此限制 Agent 停止循环。
     * 过大会导致上下文膨胀和 API 超时，过小可能回答不完整。
     */
    private static final int MAX_AGENT_ROUNDS = 8;

    @Bean
    public ReactAgent skillsAgent(
            @Qualifier("deepSeekChatModel") ChatModel chatModel,
            SkillsAgentHook skillsAgentHook,
            CalculatorToolService calculatorToolService) {

        CompileConfig compileConfig = CompileConfig.builder()
                .recursionLimit(MAX_AGENT_ROUNDS * 2)  // 每轮 = llm节点 + tool节点，框架按节点计数而非轮次
                .build();

        return ReactAgent.builder()
                .name("skills-agent")
                .model(chatModel)
                .compileConfig(compileConfig)
                .instruction("你是一个智能投资顾问助手。\n"
                        + "## 可用技能\n"
                        + "- market-analysis：市场分析\n"
                        + "- risk-assessment：风险评估\n"
                        + "- portfolio-optimization：投资组合优化\n"
                        + "- investment-recommendation：投资推荐\n"
                        + "## 规则\n"
                        + "1. 每次调用最多加载1-2个最相关的技能，不要加载全部\n"
                        + "2. 如果已经加载过某个技能，不要重复加载\n"
                        + "3. 使用 calculate 工具完成数学计算\n"
                        + "4. 直接回答用户问题，不要问用户补充信息\n"
                        + "5. 投资有风险，建议仅供参考")
                .methodTools(calculatorToolService)
                .hooks(List.of(skillsAgentHook))
                .enableLogging(true)
                .build();
    }
}
