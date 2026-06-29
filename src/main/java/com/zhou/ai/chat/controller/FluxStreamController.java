package com.zhou.ai.chat.controller;

import com.zhou.ai.chat.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Flux 响应式流式端点 — GET /api/chat/flux/stream
 * 以 Project Reactor Flux 方式流式返回回答。
 */
@RestController
@RequestMapping("/chat/flux")
public class FluxStreamController {

    private final ChatService chatService;

    public FluxStreamController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, String>> stream(
            @RequestParam String message,
            @RequestParam(defaultValue = "deepSeekChatModel") String modelName) {

        return chatService.stream(message, modelName)
                .map(chunk -> Map.of("content", chunk, "model", modelName));
    }
}
