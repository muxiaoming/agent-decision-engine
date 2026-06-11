package com.example.ai.tools.controller;

import com.example.ai.common.model.ChatResponse;
import com.example.ai.tools.service.ToolChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/tools")
public class ToolChatController {

    private final ToolChatService toolChatService;

    public ToolChatController(ToolChatService toolChatService) {
        this.toolChatService = toolChatService;
    }

    /**
     * Function Calling 对话端点 — POST /api/tools/chat
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chatWithTools(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "message 不能为空");
        }
        String modelName = request.getOrDefault("modelName", "deepSeekChatModel");
        return ResponseEntity.ok(toolChatService.chatWithTools(message, modelName));
    }
}
