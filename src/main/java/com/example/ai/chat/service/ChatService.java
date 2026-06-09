package com.example.ai.chat.service;

import com.example.ai.common.model.ChatRequest;
import com.example.ai.common.model.ChatResponse;
import com.example.ai.common.model.TokenUsage;
import com.example.ai.common.router.ModelRouter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatService {

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
                .user(message)
                .stream()
                .chatResponse()
                .map(response -> response.getResult().getOutput().getText())
                .filter(text -> text != null && !text.isEmpty());
    }
}
