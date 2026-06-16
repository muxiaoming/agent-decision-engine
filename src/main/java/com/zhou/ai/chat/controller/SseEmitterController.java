package com.zhou.ai.chat.controller;

import com.zhou.ai.chat.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SseEmitter 流式端点 — GET /api/chat/emitter/stream
 * 以 Spring MVC SseEmitter 方式流式返回回答。
 */
@RestController
@RequestMapping("/api/chat/emitter")
public class SseEmitterController {

    private final ChatService chatService;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public SseEmitterController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam String message,
            @RequestParam(defaultValue = "deepSeekChatModel") String modelName) {

        SseEmitter emitter = new SseEmitter(60_000L);

        executor.submit(() -> {
            try {
                chatService.stream(message, modelName)
                        .doOnNext(chunk -> {
                            try {
                                SseEventBuilder event = SseEmitter.event()
                                        .name("chunk")
                                        .data(Map.of("content", chunk, "model", modelName));
                                emitter.send(event);
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        })
                        .doOnComplete(() -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("complete")
                                        .data(Map.of("status", "done")));
                                emitter.complete();
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        })
                        .doOnError(emitter::completeWithError)
                        .subscribe();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(emitter::complete);

        return emitter;
    }
}
