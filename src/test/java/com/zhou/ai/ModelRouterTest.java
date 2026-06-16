package com.zhou.ai;

import com.zhou.ai.common.exception.ModelNotAvailableException;
import com.zhou.ai.common.router.ModelRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ModelRouter 纯单元测试。
 * 不依赖 Spring 上下文，使用桩实现验证路由逻辑。
 */
class ModelRouterTest {

    private ModelRouter modelRouter;

    @BeforeEach
    void setUp() {
        // 构造桩 ChatModel
        Map<String, ChatModel> chatModels = new HashMap<>();
        chatModels.put("deepSeekChatModel", new StubChatModel("deepseek"));
        chatModels.put("openAiChatModel", new StubChatModel("mimo"));
        chatModels.put("dashscopeChatModel", new StubChatModel("dashscope"));

        // 构造空的 ToolCallbackProvider
        ToolCallbackProvider emptyProvider = () -> new ToolCallback[0];

        modelRouter = new ModelRouter(chatModels, emptyProvider);
    }

    @Test
    @DisplayName("route - 正常路由到 DeepSeek 模型")
    void routeDeepSeek() {
        var client = modelRouter.route("deepSeekChatModel");
        assertNotNull(client, "路由到 deepSeekChatModel 应返回非 null ChatClient");
    }

    @Test
    @DisplayName("route - 正常路由到 MiMo 模型")
    void routeMiMo() {
        var client = modelRouter.route("openAiChatModel");
        assertNotNull(client, "路由到 openAiChatModel 应返回非 null ChatClient");
    }

    @Test
    @DisplayName("route - 正常路由到 DashScope 模型")
    void routeDashScope() {
        var client = modelRouter.route("dashscopeChatModel");
        assertNotNull(client, "路由到 dashscopeChatModel 应返回非 null ChatClient");
    }

    @Test
    @DisplayName("route - 未知模型名应抛出 ModelNotAvailableException")
    void routeThrowsForUnknown() {
        ModelNotAvailableException ex = assertThrows(
                ModelNotAvailableException.class,
                () -> modelRouter.route("nonExistentModel"));
        assertEquals("nonExistentModel", ex.getModel());
        assertTrue(ex.getMessage().contains("nonExistentModel"));
    }

    @Test
    @DisplayName("getAvailableModels - 应返回所有注册的模型名")
    void getAvailableModels() {
        List<String> models = modelRouter.getAvailableModels();
        assertEquals(3, models.size(), "应有 3 个模型");
        assertTrue(models.contains("deepSeekChatModel"));
        assertTrue(models.contains("openAiChatModel"));
        assertTrue(models.contains("dashscopeChatModel"));
    }

    /**
     * 桩 ChatModel 实现，不调用任何外部 API。
     */
    private static class StubChatModel implements ChatModel {
        private final String name;

        StubChatModel(String name) {
            this.name = name;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            // 返回一个最小化的 ChatResponse，仅供路由测试验证非 null
            return null; // 路由测试不调用 call()，只验证 route() 返回 ChatClient
        }
    }
}
