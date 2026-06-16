package com.zhou.ai.common.model;

public record TokenUsage(
        int promptTokens,
        int completionTokens,
        int totalTokens
) {
}
