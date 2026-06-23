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

    private static final String SYSTEM_PROMPT = """
            你是一个投资知识库助手，专门基于投资相关的文档和资料来回答用户的问题。
            你需要：
            1. 优先使用知识库中检索到的信息来回答
            2. 如果知识库中有相关信息，准确引用并说明来源
            3. 如果知识库中没有相关信息，请说明无法从知识库中找到，但仍可以提供一般性建议
            4. 对于财务数据和投资建议，务必声明这是基于知识库的参考信息，不构成投资建议
            5. 保持专业、客观、谨慎的态度
            """;

    private final ModelRouter modelRouter;
    private final QuestionAnswerAdvisor questionAnswerAdvisor;

    public RagService(ModelRouter modelRouter, VectorStore vectorStore) {
        this.modelRouter = modelRouter;
        this.questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore).build();
    }

    /**
     * RAG 同步问答：检索知识库 + 生成回答。
     */
    public ChatResponse ask(String message, String model) {
        ChatClient client = modelRouter.route(model);

        org.springframework.ai.chat.model.ChatResponse chatResponse = client.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .advisors(questionAnswerAdvisor)
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
                .system(SYSTEM_PROMPT)
                .user(message)
                .advisors(questionAnswerAdvisor)
                .stream()
                .chatResponse()
                .map(r -> r.getResult().getOutput().getText())
                .filter(text -> text != null && !text.isEmpty());
    }
}
