package com.example.ai.common.router;

import com.example.ai.common.exception.ModelNotAvailableException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模型路由器。
 * 注入所有 ChatModel Bean（由 Spring AI 自动配置创建），
 * 根据 modelName 动态构建 ChatClient，自动装配全部工具。
 */
@Component
public class ModelRouter {

    private final Map<String, ChatModel> chatModels;
    private final ToolCallbackProvider toolCallbackProvider;

    public ModelRouter(Map<String, ChatModel> chatModels, ToolCallbackProvider toolCallbackProvider) {
        this.chatModels = chatModels;
        this.toolCallbackProvider = toolCallbackProvider;
    }

    /**
     * 根据模型 Bean 名称路由到对应的 ChatModel，动态构建 ChatClient。
     * 自动装配全部已注册的 ToolCallback，无需硬编码工具名。
     *
     * @param modelName 模型 Bean 名称，如 deepSeekChatModel、openAiChatModel、dashscopeChatModel
     * @return 配置好工具的 ChatClient
     */
    public ChatClient route(String modelName) {
        ChatModel chatModel = chatModels.get(modelName);
        if (chatModel == null) {
            throw new ModelNotAvailableException(modelName,
                    "未知的模型: " + modelName + "，可用模型: " + String.join(", ", getAvailableModels()));
        }
        return ChatClient.builder(chatModel)
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    /**
     * 获取所有已注册的模型 Bean 名称。
     */
    public List<String> getAvailableModels() {
        return chatModels.entrySet().stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
