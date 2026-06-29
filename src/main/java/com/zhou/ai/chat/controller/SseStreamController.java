package com.zhou.ai.chat.controller;

import com.zhou.ai.chat.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * SSE 标准流式端点 — GET /api/chat/sse/stream
 * 以标准 SSE 协议流式返回回答。
 */
@RestController
@RequestMapping("/chat/sse")
public class SseStreamController {

    private final ChatService chatService;

    public SseStreamController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, String>>> stream(
            @RequestParam String message,
            @RequestParam(defaultValue = "deepSeekChatModel") String modelName) {

        return chatService.stream(message, modelName)
                .map(chunk -> ServerSentEvent.<Map<String, String>>builder()
                        .event("message")
                        .data(Map.of("content", chunk, "model", modelName))
                        .build())
                .concatWithValues(
                        ServerSentEvent.<Map<String, String>>builder()
                                .event("done")
                                .data(Map.of("status", "[DONE]"))
                                .build()
                );
    }
}
