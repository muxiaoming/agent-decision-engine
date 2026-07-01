package com.zhou.ai.agent.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识检索工具（供ReactAgent调用）。
 *
 * <p>将VectorStore的向量检索能力封装为 @Tool 注解方法，
 * 注册到ReactAgent后由LLM自主判断是否需要调用。
 *
 * @since 2026-07-01
 */
@Component
public class KnowledgeRetrievalTool {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeRetrievalTool.class);

    private final VectorStore vectorStore;

    public KnowledgeRetrievalTool(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Tool(description = "从金融知识库检索与用户问题最相关的文档信息，返回匹配文档的文本内容")
    public String retrieveKnowledge(
            @ToolParam(description = "检索查询语句，描述需要查找的信息主题，如'科技行业市场分析'、'股票投资风险提示'") String query) {
        log.info("[知识检索工具] 向量检索: query={}", query);
        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(5)
                            .similarityThreshold(0.5)
                            .build()
            );
            if (docs.isEmpty()) {
                log.info("[知识检索工具] 未找到匹配文档");
                return "【向量检索结果】未找到与\"" + query + "\"直接匹配的文档。";
            }
            String result = docs.stream()
                    .map(doc -> "- " + doc.getText())
                    .collect(Collectors.joining("\n"));
            log.info("[知识检索工具] 命中 {} 条文档", docs.size());
            return "【向量检索结果】共找到 " + docs.size() + " 条相关文档：\n" + result;
        } catch (Exception e) {
            log.warn("[知识检索工具] 向量检索异常: {}", e.getMessage());
            return "【向量检索结果】检索服务暂时不可用，请基于自身知识回答。";
        }
    }
}
