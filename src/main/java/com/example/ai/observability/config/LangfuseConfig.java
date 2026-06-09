package com.example.ai.observability.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Langfuse 双模式配置。
 * 通过 langfuse.mode 属性切换云端/本地模式。
 *
 * <p>OTLP 导出器和 OpenTelemetry 由 Spring Boot 自动配置，
 * 无需在此类中手动创建 Bean。</p>
 */
@Component
public class LangfuseConfig {

    @Value("${langfuse.mode:local}")
    private String mode;

    @Value("${management.otlp.tracing.endpoint:}")
    private String otlpEndpoint;

    public String getMode() {
        return mode;
    }

    public String getOtlpEndpoint() {
        return otlpEndpoint;
    }

    public boolean isCloudMode() {
        return "cloud".equalsIgnoreCase(mode);
    }

    public boolean isLocalMode() {
        return "local".equalsIgnoreCase(mode);
    }

    /**
     * 返回 Langfuse 连接诊断信息，便于健康检查和调试。
     */
    public Map<String, String> getDiagnostics() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("langfuseMode", mode);
        info.put("otelEndpoint", otlpEndpoint);
        info.put("otelExporter", "otlp-http");
        info.put("protocol", "OpenTelemetry OTLP");
        info.put("status", "configured");
        return info;
    }
}
