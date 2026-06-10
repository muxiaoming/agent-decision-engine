package com.example.ai;

import com.example.ai.common.model.ChatRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全端口集成测试。
 * 真实启动应用，使用 TestRestTemplate 调用 HTTP 端点。
 * AI 调用类测试做容错处理：API 不可用时跳过，不 fail。
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
        ResponseEntity<List<String>> resp = rest.exchange(
                "/api/chat/models", HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});
        assertEquals(200, resp.getStatusCode().value());
        List<String> models = resp.getBody();
        assertNotNull(models);
        assertFalse(models.isEmpty(), "模型列表不应为空");
        System.out.println("可用模型: " + models);
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/chat - 同步聊天（DeepSeek）")
    void chatSync() {
        ChatRequest req = new ChatRequest("你好，用一句话介绍自己", null, null);
        try {
            ResponseEntity<Map> resp = rest.postForEntity("/api/chat?modelName=deepSeekChatModel", req, Map.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                assertNotNull(resp.getBody().get("content"), "应返回 content");
                System.out.println("DeepSeek 回复: " + resp.getBody().get("content"));
            } else {
                System.out.println("DeepSeek API 不可用，状态码: " + resp.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("DeepSeek API 调用失败（可能未配置 key）: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/chat/flux/stream - Flux 流式输出")
    void fluxStream() {
        try {
            ResponseEntity<String> resp = rest.getForEntity(
                    "/api/chat/flux/stream?message=你好&modelName=deepSeekChatModel", String.class);
            assertEquals(200, resp.getStatusCode().value());
            System.out.println("Flux 流式响应长度: " + (resp.getBody() != null ? resp.getBody().length() : 0));
        } catch (Exception e) {
            System.out.println("Flux 流式调用失败: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/chat/sse/stream - SSE 流式输出")
    void sseStream() {
        try {
            ResponseEntity<String> resp = rest.getForEntity(
                    "/api/chat/sse/stream?message=你好&modelName=deepSeekChatModel", String.class);
            assertEquals(200, resp.getStatusCode().value());
            System.out.println("SSE 流式响应长度: " + (resp.getBody() != null ? resp.getBody().length() : 0));
        } catch (Exception e) {
            System.out.println("SSE 流式调用失败: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/chat/emitter/stream - SseEmitter 流式输出")
    void emitterStream() {
        try {
            ResponseEntity<String> resp = rest.getForEntity(
                    "/api/chat/emitter/stream?message=你好&modelName=deepSeekChatModel", String.class);
            assertEquals(200, resp.getStatusCode().value());
            System.out.println("Emitter 流式响应长度: " + (resp.getBody() != null ? resp.getBody().length() : 0));
        } catch (Exception e) {
            System.out.println("Emitter 流式调用失败: " + e.getMessage());
        }
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
            if (resp.getStatusCode().is2xxSuccessful()) {
                Map body = resp.getBody();
                assertNotNull(body.get("traceId"), "应返回 traceId");
                assertNotNull(body.get("content"), "应返回模型回复");
                System.out.println("MiMo Trace ID: " + body.get("traceId"));
            } else {
                System.out.println("MiMo 测试端点返回: " + resp.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("MiMo API 不可用: " + e.getMessage());
        }
    }

    @Test
    @Order(10)
    @DisplayName("GET /api/observability/test-deepseek - DeepSeek 模型 Trace 测试")
    void observabilityTestDeepSeek() {
        try {
            ResponseEntity<Map> resp = rest.getForEntity("/api/observability/test-deepseek", Map.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                Map body = resp.getBody();
                assertNotNull(body.get("traceId"), "应返回 traceId");
                System.out.println("DeepSeek Trace ID: " + body.get("traceId"));
            } else {
                System.out.println("DeepSeek 测试端点返回: " + resp.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("DeepSeek API 不可用: " + e.getMessage());
        }
    }

    // ==================== RAG 模块 ====================

    @Test
    @Order(11)
    @DisplayName("POST /api/rag/ingest + POST /api/rag/ask - RAG 完整流程")
    void ragIngestAndAsk() {
        try {
            // 先摄入示例文档
            org.springframework.core.io.FileSystemResource fileResource =
                    new org.springframework.core.io.FileSystemResource("src/main/resources/docs/sample.txt");
            org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("file", fileResource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ResponseEntity<Map> ingestResp = rest.postForEntity("/api/rag/ingest",
                    new HttpEntity<>(body, headers), Map.class);
            System.out.println("RAG 摄入结果: " + ingestResp.getBody());

            // 再问答
            ChatRequest req = new ChatRequest("这个文档讲了什么？", null, null);
            ResponseEntity<Map> askResp = rest.postForEntity(
                    "/api/rag/ask?modelName=deepSeekChatModel", req, Map.class);
            if (askResp.getStatusCode().is2xxSuccessful()) {
                assertNotNull(askResp.getBody().get("content"), "RAG 应返回回答");
                System.out.println("RAG 回答: " + askResp.getBody().get("content"));
            } else {
                System.out.println("RAG 问答返回: " + askResp.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("RAG 测试失败（可能 API 不可用）: " + e.getMessage());
        }
    }

    // ==================== Tools 模块 ====================

    @Test
    @Order(12)
    @DisplayName("POST /api/tools/chat - Function Calling 工具调用")
    void toolsChat() {
        try {
            ChatRequest req = new ChatRequest("北京今天天气怎么样？", null, null);
            ResponseEntity<Map> resp = rest.postForEntity(
                    "/api/tools/chat?modelName=deepSeekChatModel", req, Map.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                assertNotNull(resp.getBody().get("content"), "工具调用应返回回答");
                System.out.println("工具调用回答: " + resp.getBody().get("content"));
            } else {
                System.out.println("工具调用返回: " + resp.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("工具调用测试失败: " + e.getMessage());
        }
    }
}
