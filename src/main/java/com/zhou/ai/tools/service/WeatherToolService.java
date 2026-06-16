package com.zhou.ai.tools.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 天气查询工具服务。
 * 使用 @Tool 注解声明工具能力，Spring AI 自动注册为可调用工具。
 */
@Component
public class WeatherToolService {

    private static final Map<String, WeatherInfo> MOCK_DATA = Map.of(
            "北京", new WeatherInfo("北京", 25, "°C", "晴"),
            "上海", new WeatherInfo("上海", 28, "°C", "多云"),
            "广州", new WeatherInfo("广州", 32, "°C", "雷阵雨"),
            "深圳", new WeatherInfo("深圳", 30, "°C", "阴")
    );

    @Tool(description = "查询指定城市的当前天气信息")
    public WeatherInfo queryWeather(
            @ToolParam(description = "城市名称，如北京、上海") String location) {
        return MOCK_DATA.getOrDefault(location, new WeatherInfo(location, 20, "°C", "未知"));
    }

    /**
     * 天气信息记录。
     *
     * @param location    城市名称
     * @param temperature 温度值
     * @param unit        温度单位
     * @param condition   天气状况
     */
    public record WeatherInfo(String location, int temperature, String unit, String condition) {
    }
}
