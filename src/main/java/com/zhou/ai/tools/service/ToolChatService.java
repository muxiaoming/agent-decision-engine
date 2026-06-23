package com.zhou.ai.tools.service;

import com.zhou.ai.common.model.ChatResponse;
import com.zhou.ai.common.model.TokenUsage;
import com.zhou.ai.common.router.ModelRouter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.stereotype.Service;

/**
 * 工具调用服务。
 * 通过 ModelRouter 动态构建 ChatClient，
 * 工具已在 ModelRouter 中通过 ToolCallbackProvider 自动装配。
 */
@Service
public class ToolChatService {

    private static final String SYSTEM_PROMPT = """
            你是一个智能投资助手，拥有以下工具可以查询实时数据和进行计算：

            【股价查询工具】
            - getStockPrice: 查询股票实时价格
            - getStockHistory: 查询股票历史价格
            - calculateReturn: 计算股票投资收益率

            【市场指标工具】
            - getMarketIndex: 查询大盘指数（上证指数、深成指等）
            - getMarketVolatility: 查询市场波动率（VIX）
            - getMarketSentiment: 查询市场情绪指标
            - getSectorPerformance: 查询行业板块表现

            【风险计算工具】
            - calculatePortfolioReturn: 计算投资组合预期收益
            - calculateValueAtRisk: 计算VaR（在险价值）
            - calculateSharpeRatio: 计算夏普比率

            使用工具的原则：
            1. 当用户询问股票相关信息时，优先使用股价查询工具获取实时数据
            2. 当用户询问大盘或市场整体情况时，使用市场指标工具
            3. 当用户需要计算收益、风险或优化配置时，使用风险计算工具
            4. 工具调用前先理解用户需求，调用后给出专业分析和建议
            5. 始终声明投资有风险，建议仅供参考
            """;

    private final ModelRouter modelRouter;

    public ToolChatService(ModelRouter modelRouter) {
        this.modelRouter = modelRouter;
    }

    /**
     * 带工具调用的对话。
     * ChatClient 已在 ModelRouter 中自动装配全部工具，无需硬编码工具名。
     */
    public ChatResponse chatWithTools(String message, String modelName) {
        ChatClient client = modelRouter.route(modelName);

        ChatClient.CallResponseSpec callResponse = client.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .call();

        org.springframework.ai.chat.model.ChatResponse chatResponse = callResponse.chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        Usage usage = chatResponse.getMetadata().getUsage();

        TokenUsage tokenUsage = new TokenUsage(
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getTotalTokens()
        );

        return new ChatResponse(content, modelName, null, System.currentTimeMillis(), tokenUsage);
    }
}
