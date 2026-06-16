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
