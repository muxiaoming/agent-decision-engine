package com.zhou.ai.observability.controller;

import com.zhou.ai.common.model.TokenUsage;
import com.zhou.ai.common.router.ModelRouter;
import com.zhou.ai.observability.config.LangfuseConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Langfuse 可观测性验证控制器。
 * HTTP span 自动作为根 span（由 opentelemetry-spring-boot-starter 创建）。
 */
@RestController
@RequestMapping("/api/observability")
public class LangfuseTestController {

    private static final String MIMO_MODEL = "openAiChatModel";

    private final ModelRouter modelRouter;
    private final LangfuseConfig langfuseConfig;

    public LangfuseTestController(ModelRouter modelRouter, LangfuseConfig langfuseConfig) {
        this.modelRouter = modelRouter;
        this.langfuseConfig = langfuseConfig;
    }

    /**
     * 健康检查 — GET /api/observability/health
     * 返回 Langfuse 连接配置和 OTel 导出器状态。
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>(langfuseConfig.getDiagnostics());
        result.put("tip", "发送 AI 请求后，在 Langfuse 界面查看 Trace（预计 30 秒内可见）");
        return ResponseEntity.ok(result);
    }

    /**
     * MiMo 模型测试 — GET /api/observability/test-mimo
     * 使用 MiMo 模型发送简单对话，验证 Langfuse Trace 上报。
     *
     * @param message 测试消息
     * @return 模型回答、Token 用量和 Trace ID
     */
    @GetMapping("/test-mimo")
    public ResponseEntity<Map<String, Object>> testMiMo(
            @RequestParam(defaultValue = "你好，请用一句话介绍自己") String message) {
        return doTest("openAiChatModel", "xiaomi/mimo-v2.5-pro", message);
    }

    /**
     * DeepSeek 模型测试 — GET /api/observability/test-deepseek
     * 使用 DeepSeek 模型发送简单对话，验证 Langfuse Trace 上报。
     *
     * @param message 测试消息
     * @return 模型回答、Token 用量和 Trace ID
     */
    @GetMapping("/test-deepseek")
    public ResponseEntity<Map<String, Object>> testDeepSeek(
            @RequestParam(defaultValue = "你好，请用一句话介绍自己") String message) {
        return doTest("deepSeekChatModel", "deepseek-chat", message);
    }

    @GetMapping("/test-dashscope")
    public ResponseEntity<Map<String, Object>> testDashscope(
            @RequestParam(defaultValue = "你好，请用一句话介绍自己") String message) {
        return doTest("dashscopeChatModel", "dashscope-chat", message);
    }

    /**
     * 通用测试方法：调用指定模型并返回结果。
     * 不再手动创建 Span，HTTP 请求由 OTel 自动探针自动作为根 span。
     */
    private ResponseEntity<Map<String, Object>> doTest(
            String modelName, String modelDisplayName, String message) {
        long startTime = System.currentTimeMillis();

        ChatClient client = modelRouter.route(modelName);

        org.springframework.ai.chat.model.ChatResponse chatResponse = client.prompt()
                .user(message)
                .call()
                .chatResponse();

        String content = chatResponse.getResult().getOutput().getText();
        Usage usage = chatResponse.getMetadata().getUsage();
        long durationMs = System.currentTimeMillis() - startTime;

        TokenUsage tokenUsage = new TokenUsage(
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getTotalTokens()
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model", modelDisplayName);
        result.put("content", content);
        result.put("tokenUsage", tokenUsage);
        result.put("durationMs", durationMs);
        result.put("langfuseMode", langfuseConfig.getMode());
        result.put("langfuseUrl", langfuseConfig.isLocalMode()
                ? "http://localhost:3000"
                : "https://cloud.langfuse.com");
        result.put("tip", "在 Langfuse 界面查看 Trace（HTTP root span + AI GENERATION child span）");

        return ResponseEntity.ok(result);
    }

    /**
     * 可用模型列表 — GET /api/observability/models
     */
    @GetMapping("/models")
    public ResponseEntity<Map<String, Object>> models() {
        List<String> availableModels = modelRouter.getAvailableModels();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("availableModels", availableModels);
        result.put("total", availableModels.size());
        result.put("defaultTestModel", MIMO_MODEL);

        return ResponseEntity.ok(result);
    }
}
