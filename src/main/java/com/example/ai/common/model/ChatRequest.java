package com.example.ai.common.model;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "消息内容不能为空")
        String message,

        String model,

        String chatId
) {
}
