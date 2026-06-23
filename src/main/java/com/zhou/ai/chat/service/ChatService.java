package com.zhou.ai.chat.service;

import com.zhou.ai.common.model.ChatRequest;
import com.zhou.ai.common.model.ChatResponse;
import com.zhou.ai.common.model.TokenUsage;
import com.zhou.ai.common.router.ModelRouter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            你是一个智能投资顾问助手，专注于为用户提供专业的投资建议和财务分析。
            你需要：
            1. 理解用户的投资需求和风险偏好
            2. 基于真实数据和专业知识给出建议
            3. 说明投资风险，避免过度承诺
            4. 用通俗易懂的语言解释复杂的金融概念
            5. 如果不确定或信息不足，请说明需要进一步了解
            """;

    private final ModelRouter modelRouter;

    public ChatService(ModelRouter modelRouter) {
        this.modelRouter = modelRouter;
    }

    /**
     * 同步对话：根据 modelName 路由到对应模型并返回完整回答。
     */
    public ChatResponse chat(ChatRequest request, String modelName) {
        ChatClient client = modelRouter.route(modelName);

        org.springframework.ai.chat.model.ChatResponse chatResponse = client.prompt()
                .system(SYSTEM_PROMPT)
                .user(request.message())
                .call()
                .chatResponse();

        String content = chatResponse.getResult().getOutput().getText();
        Usage usage = chatResponse.getMetadata().getUsage();

        TokenUsage tokenUsage = new TokenUsage(
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getTotalTokens()
        );

        return new ChatResponse(
                content,
                modelName,
                request.chatId(),
                System.currentTimeMillis(),
                tokenUsage
        );
    }

    /**
     * 流式对话：根据 modelName 路由到对应模型并返回 Flux 流式回答。
     */
    public Flux<String> stream(String message, String modelName) {
        ChatClient client = modelRouter.route(modelName);

        return client.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .stream()
                .chatResponse()
                .map(response -> response.getResult().getOutput().getText())
                .filter(text -> text != null && !text.isEmpty());
    }
}
