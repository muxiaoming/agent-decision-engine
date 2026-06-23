package com.zhou.ai.tools;

import com.zhou.ai.tools.service.StockPriceToolService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StockPriceToolService 单元测试。
 */
@SpringBootTest
class StockPriceToolServiceTest {

    @Autowired
    private StockPriceToolService stockPriceToolService;

    @Test
    void shouldReturnStockPriceForAAPL() {
        Map<String, Object> result = stockPriceToolService.getStockPrice("AAPL");

        assertNotNull(result);
        assertEquals("AAPL", result.get("symbol"));
        assertEquals("Apple Inc.", result.get("name"));
        assertNotNull(result.get("price"));
        assertTrue((Double) result.get("price") > 0);
    }

    @Test
    void shouldReturnStockPriceForMSFT() {
        Map<String, Object> result = stockPriceToolService.getStockPrice("MSFT");

        assertNotNull(result);
        assertEquals("MSFT", result.get("symbol"));
        assertEquals("Microsoft Corporation", result.get("name"));
    }

    @Test
    void shouldHandleUnknownSymbol() {
        Map<String, Object> result = stockPriceToolService.getStockPrice("UNKNOWN");

        assertNotNull(result);
        assertEquals("UNKNOWN", result.get("symbol"));
        assertNotNull(result.get("error"));
    }

    @Test
    void shouldReturnStockHistory() {
        Map<String, Object> result = stockPriceToolService.getStockHistory("AAPL", 30);

        assertNotNull(result);
        assertEquals("AAPL", result.get("symbol"));
        assertEquals("30天", result.get("period"));
        assertNotNull(result.get("data"));
    }

    @Test
    void shouldCalculateReturn() {
        Map<String, Object> result = stockPriceToolService.calculateReturn("AAPL", 175.0, 100);

        assertNotNull(result);
        assertEquals("AAPL", result.get("symbol"));
        assertNotNull(result.get("totalCost"));
        assertNotNull(result.get("currentValue"));
        assertNotNull(result.get("profit"));
        assertNotNull(result.get("returnRate"));
    }
}
