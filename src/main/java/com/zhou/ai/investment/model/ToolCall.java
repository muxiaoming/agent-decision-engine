package com.zhou.ai.investment.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * 工具调用记录 DTO。
 * 记录每个工具调用的输入、输出和执行时间。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ToolCall(
        /**
         * 工具名称。
         */
        String toolName,

        /**
         * 工具输入参数。
         */
        Map<String, Object> input,

        /**
         * 工具输出结果。
         */
        Map<String, Object> output,

        /**
         * 执行耗时（毫秒）。
         */
        long durationMs,

        /**
         * 调用状态。
         * 可选值: "success", "failed"
         */
        String status
) {
    /**
     * 创建成功的工具调用记录。
     */
    public static ToolCall success(String toolName, Map<String, Object> input,
                                   Map<String, Object> output, long durationMs) {
        return new ToolCall(toolName, input, output, durationMs, "success");
    }

    /**
     * 创建失败的工具调用记录。
     */
    public static ToolCall failed(String toolName, Map<String, Object> input,
                                  String error, long durationMs) {
        return new ToolCall(toolName, input, Map.of("error", error), durationMs, "failed");
    }

    /**
     * 判断调用是否成功。
     */
    public boolean isSuccessful() {
        return "success".equals(status);
    }

    /**
     * 从输出中获取字符串值。
     */
    public String getOutputAsString(String key) {
        if (output == null || !output.containsKey(key)) {
            return null;
        }
        Object value = output.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 从输出中获取 double 值。
     */
    public double getOutputAsDouble(String key, double defaultValue) {
        if (output == null || !output.containsKey(key)) {
            return defaultValue;
        }
        Object value = output.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
