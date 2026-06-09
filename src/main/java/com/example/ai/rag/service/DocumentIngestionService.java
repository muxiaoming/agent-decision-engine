package com.example.ai.rag.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
public class DocumentIngestionService {

    private final VectorStore vectorStore;

    public DocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 摄入文档到向量知识库。
     * 流程：读取文件 → TokenTextSplitter 分割 → 写入 VectorStore
     */
    public Map<String, Object> ingest(MultipartFile file) {
        try {
            TextReader reader = new TextReader(file.getResource());
            List<Document> documents = reader.get();

            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> chunks = splitter.apply(documents);

            String documentId = "doc-" + System.currentTimeMillis();
            for (int i = 0; i < chunks.size(); i++) {
                chunks.get(i).getMetadata().put("documentId", documentId);
                chunks.get(i).getMetadata().put("source", file.getOriginalFilename());
            }

            vectorStore.add(chunks);

            return Map.of(
                    "status", "success",
                    "documentId", documentId,
                    "chunks", chunks.size(),
                    "message", "文档已成功摄入知识库"
            );
        } catch (Exception e) {
            return Map.of(
                    "status", "error",
                    "message", "文档摄入失败: " + e.getMessage()
            );
        }
    }
}
