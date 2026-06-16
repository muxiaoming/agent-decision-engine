package com.zhou.ai.rag.service;

import com.zhou.ai.common.model.ChatResponse;
import com.zhou.ai.common.model.TokenUsage;
import com.zhou.ai.common.router.ModelRouter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class RagService {

    private final ModelRouter modelRouter;
    private final VectorStore vectorStore;

    public RagService(ModelRouter modelRouter, VectorStore vectorStore) {
        this.modelRouter = modelRouter;
        this.vectorStore = vectorStore;
    }

    /**
     * RAG 同步问答：检索知识库 + 生成回答。
     */
    public ChatResponse ask(String message, String model) {
        ChatClient client = modelRouter.route(model);

        org.springframework.ai.chat.model.ChatResponse chatResponse = client.prompt()
                .user(message)
                .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .call()
                .chatResponse();

        String content = chatResponse.getResult().getOutput().getText();
        Usage usage = chatResponse.getMetadata().getUsage();

        TokenUsage tokenUsage = new TokenUsage(
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getTotalTokens()
        );

        return new ChatResponse(content, model, null, System.currentTimeMillis(), tokenUsage);
    }

    /**
     * RAG 流式问答。
     */
    public Flux<String> askStream(String message, String model) {
        ChatClient client = modelRouter.route(model);

        return client.prompt()
                .user(message)
                .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .stream()
                .chatResponse()
                .map(r -> r.getResult().getOutput().getText())
                .filter(text -> text != null && !text.isEmpty());
    }
}
