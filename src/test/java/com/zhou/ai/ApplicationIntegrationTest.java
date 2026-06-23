package com.zhou.ai;

import com.zhou.ai.common.model.ChatRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * 全端口集成测试。
 * 真实启动应用，使用 TestRestTemplate 调用 HTTP 端点。
 * AI 调用类测试使用 Assumptions：API 不可用时跳过（显示为 skipped 而非 passed）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApplicationIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    // ==================== Chat 模块 ====================

    @Test
    @Order(1)
    @DisplayName("GET /api/chat/models - 获取可用模型列表")
    void chatModels() {
        ResponseEntity<Map> resp = rest.getForEntity("/api/chat/models", Map.class);
        assertEquals(200, resp.getStatusCode().value());
        Map body = resp.getBody();
        assertNotNull(body);
        assertNotNull(body.get("models"), "应包含 models 字段");
        System.out.println("可用模型: " + body.get("models"));
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/chat - 同步聊天（DeepSeek）")
    void chatSync() {
        ChatRequest req = new ChatRequest("你好，用一句话介绍自己", null, null);
        ResponseEntity<Map> resp = rest.postForEntity("/api/chat?modelName=deepSeekChatModel", req, Map.class);
        assumeTrue(resp.getStatusCode().is2xxSuccessful(), "DeepSeek API 不可用，跳过测试");
        assertNotNull(resp.getBody().get("content"), "应返回 content");
        System.out.println("DeepSeek 回复: " + resp.getBody().get("content"));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/chat/flux/stream - Flux 流式输出")
    void fluxStream() {
        ResponseEntity<String> resp = rest.getForEntity(
                "/api/chat/flux/stream?message=你好&modelName=deepSeekChatModel", String.class);
        assumeTrue(resp.getStatusCode().is2xxSuccessful(), "Flux 流式 API 不可用，跳过测试");
        assertEquals(200, resp.getStatusCode().value());
        System.out.println("Flux 流式响应长度: " + (resp.getBody() != null ? resp.getBody().length() : 0));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/chat/sse/stream - SSE 流式输出")
    void sseStream() {
        ResponseEntity<String> resp = rest.getForEntity(
                "/api/chat/sse/stream?message=你好&modelName=deepSeekChatModel", String.class);
        assumeTrue(resp.getStatusCode().is2xxSuccessful(), "SSE 流式 API 不可用，跳过测试");
        assertEquals(200, resp.getStatusCode().value());
        System.out.println("SSE 流式响应长度: " + (resp.getBody() != null ? resp.getBody().length() : 0));
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/chat/emitter/stream - SseEmitter 流式输出")
    void emitterStream() {
        ResponseEntity<String> resp = rest.getForEntity(
                "/api/chat/emitter/stream?message=你好&modelName=deepSeekChatModel", String.class);
        assumeTrue(resp.getStatusCode().is2xxSuccessful(), "Emitter 流式 API 不可用，跳过测试");
        assertEquals(200, resp.getStatusCode().value());
        System.out.println("Emitter 流式响应长度: " + (resp.getBody() != null ? resp.getBody().length() : 0));
    }

    // ==================== Graph 模块 ====================

    @Test
    @Order(6)
    @DisplayName("POST /api/graph/execute - Graph 工作流执行")
    void graphExecute() {
        Map<String, String> req = Map.of("input", "如何学习编程技术");
        ResponseEntity<Map> resp = rest.postForEntity("/api/graph/execute", req, Map.class);
        assertEquals(200, resp.getStatusCode().value());
        Map body = resp.getBody();
        assertNotNull(body);
        assertEquals("technical", body.get("branch"), "应分类为 technical");
        assertNotNull(body.get("output"));
        System.out.println("Graph 分类: " + body.get("branch") + ", 输出: " + body.get("output"));
    }

    // ==================== Observability 模块 ====================

    @Test
    @Order(7)
    @DisplayName("GET /api/observability/health - 可观测性健康检查")
    void observabilityHealth() {
        ResponseEntity<Map> resp = rest.getForEntity("/api/observability/health", Map.class);
        assertEquals(200, resp.getStatusCode().value());
        Map body = resp.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("langfuseMode"), "应包含 langfuseMode");
        assertTrue(body.containsKey("otelEndpoint"), "应包含 otelEndpoint");
        System.out.println("健康检查: " + body);
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/observability/models - 可观测性模型列表")
    void observabilityModels() {
        ResponseEntity<Map> resp = rest.getForEntity("/api/observability/models", Map.class);
        assertEquals(200, resp.getStatusCode().value());
        Map body = resp.getBody();
        assertNotNull(body);
        assertTrue((Integer) body.get("total") > 0, "模型数量应大于 0");
        System.out.println("观测模型列表: " + body.get("availableModels"));
    }

    @Test
    @Order(9)
    @DisplayName("GET /api/observability/test-mimo - MiMo 模型 Trace 测试")
    void observabilityTestMimo() {
        try {
            ResponseEntity<Map> resp = rest.getForEntity("/api/observability/test-mimo", Map.class);
            assumeTrue(resp.getStatusCode().is2xxSuccessful(), "MiMo API 不可用，跳过测试");
            Map body = resp.getBody();
            assertNotNull(body.get("content"), "应返回模型回复");
            assertNotNull(body.get("langfuseMode"), "应返回 langfuseMode");
            System.out.println("MiMo 测试成功，langfuseMode: " + body.get("langfuseMode"));
        } catch (Exception e) {
            System.out.println("MiMo API 不可用，跳过测试: " + e.getMessage());
            assumeTrue(false, "MiMo API 不可用，跳过测试");
        }
    }

    @Test
    @Order(10)
    @DisplayName("GET /api/observability/test-deepseek - DeepSeek 模型 Trace 测试")
    void observabilityTestDeepSeek() {
        try {
            ResponseEntity<Map> resp = rest.getForEntity("/api/observability/test-deepseek", Map.class);
            assumeTrue(resp.getStatusCode().is2xxSuccessful(), "DeepSeek API 不可用，跳过测试");
            Map body = resp.getBody();
            assertNotNull(body.get("content"), "应返回模型回复");
            assertNotNull(body.get("langfuseMode"), "应返回 langfuseMode");
            System.out.println("DeepSeek 测试成功，langfuseMode: " + body.get("langfuseMode"));
        } catch (Exception e) {
            System.out.println("DeepSeek API 不可用，跳过测试: " + e.getMessage());
            assumeTrue(false, "DeepSeek API 不可用，跳过测试");
        }
    }

    // ==================== RAG 模块 ====================

    @Test
    @Order(11)
    @DisplayName("POST /api/rag/ingest + POST /api/rag/ask - RAG 完整流程")
    void ragIngestAndAsk() {
        // 摄入示例文档
        org.springframework.core.io.FileSystemResource fileResource =
                new org.springframework.core.io.FileSystemResource("src/main/resources/docs/sample.txt");
        LinkedMultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("file", fileResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<Map> ingestResp = rest.postForEntity("/api/rag/ingest",
                new HttpEntity<>(multipart, headers), Map.class);
        assertEquals(200, ingestResp.getStatusCode().value(), "文档摄入应返回 200");
        System.out.println("RAG 摄入结果: " + ingestResp.getBody());

        // 问答
        ChatRequest req = new ChatRequest("这个文档讲了什么？", null, null);
        ResponseEntity<Map> askResp = rest.postForEntity(
                "/api/rag/ask?modelName=deepSeekChatModel", req, Map.class);
        assumeTrue(askResp.getStatusCode().is2xxSuccessful(), "RAG 问答 API 不可用，跳过测试");
        assertNotNull(askResp.getBody().get("content"), "RAG 应返回回答");
        System.out.println("RAG 回答: " + askResp.getBody().get("content"));
    }

    // ==================== Tools 模块 ====================

    @Test
    @Order(12)
    @DisplayName("POST /api/tools/chat - Function Calling 工具调用")
    void toolsChat() {
        ChatRequest req = new ChatRequest("北京今天天气怎么样？", null, null);
        ResponseEntity<Map> resp = rest.postForEntity(
                "/api/tools/chat?modelName=deepSeekChatModel", req, Map.class);
        assumeTrue(resp.getStatusCode().is2xxSuccessful(), "工具调用 API 不可用，跳过测试");
        assertNotNull(resp.getBody().get("content"), "工具调用应返回回答");
        System.out.println("工具调用回答: " + resp.getBody().get("content"));
    }
}
