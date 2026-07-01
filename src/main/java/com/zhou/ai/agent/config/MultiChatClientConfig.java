package com.zhou.ai.agent.config;

import com.zhou.ai.agent.node.IntentClassifyAgent;
import com.zhou.ai.common.router.ModelRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 多Agent专用ChatClient配置类。
 *
 * <p>为仍需ChatClient的Agent（当前仅{@link IntentClassifyAgent}）创建命名Bean。
 * 其他Agent（ProblemPerception/KnowledgeRetrieval/DataFetch/ReasoningAnalysis/
 * DecisionGenerate/GraphSchedule）已改为使用{@link ReactAgentFactory}管理的
 * ReactAgent，不再需要独立ChatClient。
 *
 * @since 2026-06-30
 */
@Configuration
public class MultiChatClientConfig {

    private static final Logger log = LoggerFactory.getLogger(MultiChatClientConfig.class);

    private static final String DEFAULT_MODEL = "openAiChatModel";

    /**
     * -- 意图分类Agent系统提示词 --
     *
     * 职责：判断用户输入是否为金融投资相关，输出结构化分类结果。
     */
    private static final String INTENT_CLASSIFY_SYSTEM_PROMPT = """
            你是一个严格的问题分类器。只做一件事：判断用户输入是否与金融投资相关。

            金融投资相关话题包括：股票、基金、债券、ETF、理财、资产配置、风险评估、
            市场分析、行业研究、仓位管理、定投、交易策略、收益率、估值等。

            非投资话题包括：问候寒暄、天气、娱乐、编程技术问题等。

            输出规则：
            - 在回复的开头单独一行输出判断结果
            - 如果与投资相关，输出格式为：INVESTMENT:true
            - 如果与投资无关，输出格式为：INVESTMENT:false
            - 第二行开始输出简短理由
            """;

    private final ModelRouter modelRouter;

    public MultiChatClientConfig(ModelRouter modelRouter) {
        this.modelRouter = modelRouter;
    }

    private ChatClient createClient(String systemPrompt) {
        return modelRouter.route(DEFAULT_MODEL)
                .mutate()
                .defaultSystem(systemPrompt)
                .build();
    }

    @Bean("intentClassifyChatClient")
    public ChatClient intentClassifyChatClient() {
        log.info("初始化意图分类Agent ChatClient");
        return createClient(INTENT_CLASSIFY_SYSTEM_PROMPT);
    }
}