package com.zhou.ai.tools;

import com.zhou.ai.tools.service.MarketIndexToolService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MarketIndexToolService 单元测试。
 */
@SpringBootTest
class MarketIndexToolServiceTest {

    @Autowired
    private MarketIndexToolService marketIndexToolService;

    @Test
    void shouldReturnShanghaiIndex() {
        Map<String, Object> result = marketIndexToolService.getMarketIndex("上证指数");

        assertNotNull(result);
        assertEquals("上证指数", result.get("name"));
        assertNotNull(result.get("value"));
        assertNotNull(result.get("changePercent"));
    }

    @Test
    void shouldReturnShenzhenIndex() {
        Map<String, Object> result = marketIndexToolService.getMarketIndex("深成指");

        assertNotNull(result);
        assertEquals("深成指", result.get("name"));
    }

    @Test
    void shouldReturnChiNextIndex() {
        Map<String, Object> result = marketIndexToolService.getMarketIndex("创业板指");

        assertNotNull(result);
        assertEquals("创业板指", result.get("name"));
    }

    @Test
    void shouldHandleUnknownIndex() {
        Map<String, Object> result = marketIndexToolService.getMarketIndex("未知指数");

        assertNotNull(result);
        assertNotNull(result.get("error"));
    }

    @Test
    void shouldReturnMarketVolatility() {
        Map<String, Object> result = marketIndexToolService.getMarketVolatility();

        assertNotNull(result);
        assertNotNull(result.get("vix"));
        assertNotNull(result.get("riskLevel"));
        assertNotNull(result.get("description"));
    }

    @Test
    void shouldReturnMarketSentiment() {
        Map<String, Object> result = marketIndexToolService.getMarketSentiment();

        assertNotNull(result);
        assertNotNull(result.get("advancers"));
        assertNotNull(result.get("decliners"));
        assertNotNull(result.get("sentiment"));
    }

    @Test
    void shouldReturnSectorPerformance() {
        Map<String, Object> result = marketIndexToolService.getSectorPerformance("科技");

        assertNotNull(result);
        assertEquals("科技板块", result.get("name"));
        assertNotNull(result.get("changePercent"));
        assertNotNull(result.get("topStocks"));
    }

    @Test
    void shouldHandleUnknownSector() {
        Map<String, Object> result = marketIndexToolService.getSectorPerformance("未知行业");

        assertNotNull(result);
        assertNotNull(result.get("error"));
    }
}
