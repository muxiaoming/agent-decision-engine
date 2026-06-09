package com.example.ai.rag.controller;

import com.example.ai.common.model.ChatResponse;
import com.example.ai.rag.service.DocumentIngestionService;
import com.example.ai.rag.service.RagService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;
    private final DocumentIngestionService ingestionService;

    public RagController(RagService ragService, DocumentIngestionService ingestionService) {
        this.ragService = ragService;
        this.ingestionService = ingestionService;
    }

    /**
     * RAG 同步问答 — POST /api/rag/ask
     */
    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> ask(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String modelName = request.getOrDefault("modelName", "deepSeekChatModel");
        return ResponseEntity.ok(ragService.ask(message, modelName));
    }

    /**
     * RAG 流式问答 — GET /api/rag/ask/stream
     */
    @GetMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, String>> askStream(
            @RequestParam String message,
            @RequestParam(defaultValue = "deepSeekChatModel") String modelName) {
        return ragService.askStream(message, modelName)
                .map(chunk -> Map.of("content", chunk, "model", modelName));
    }

    /**
     * 文档摄入 — POST /api/rag/ingest
     */
    @PostMapping("/ingest")
    public ResponseEntity<Map<String, Object>> ingest(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ingestionService.ingest(file));
    }
}
