package com.zhou.ai.tools.config;

import com.zhou.ai.tools.service.CalculatorToolService;
import com.zhou.ai.tools.service.WeatherToolService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工具回调配置。
 * 通过 MethodToolCallbackProvider 自动发现所有 @Tool 注解的方法，
 * 统一注册为 ToolCallback，无需硬编码工具名称。
 */
@Configuration
public class ToolCallbackConfig {

    @Bean
    public ToolCallbackProvider toolCallbackProvider(
            WeatherToolService weatherToolService,
            CalculatorToolService calculatorToolService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(weatherToolService, calculatorToolService)
                .build();
    }
}
