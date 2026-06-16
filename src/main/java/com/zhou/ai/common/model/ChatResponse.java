package com.zhou.ai.common.model;

public record ChatResponse(
        String content,
        String model,
        String chatId,
        long timestamp,
        TokenUsage tokenUsage
) {
}
