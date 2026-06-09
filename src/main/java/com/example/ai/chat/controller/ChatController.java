package com.example.ai.chat.controller;

import com.example.ai.chat.service.ChatService;
import com.example.ai.common.model.ChatRequest;
import com.example.ai.common.model.ChatResponse;
import com.example.ai.common.router.ModelRouter;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final ModelRouter modelRouter;

    public ChatController(ChatService chatService, ModelRouter modelRouter) {
        this.chatService = chatService;
        this.modelRouter = modelRouter;
    }

    /**
     * 同步对话端点 — POST /api/chat
     * 使用指定模型返回完整回答。
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @RequestParam(defaultValue = "deepSeekChatModel") String modelName) {
        ChatResponse response = chatService.chat(request, modelName);
        return ResponseEntity.ok(response);
    }

    /**
     * 查询所有可用模型 — GET /api/chat/models
     */
    @GetMapping("/models")
    public ResponseEntity<Map<String, List<String>>> getAvailableModels() {
        return ResponseEntity.ok(Map.of("models", modelRouter.getAvailableModels()));
    }
}
