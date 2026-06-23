package com.zhou.ai.tools;

import com.zhou.ai.tools.service.RiskCalculatorToolService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RiskCalculatorToolService 单元测试。
 */
@SpringBootTest
class RiskCalculatorToolServiceTest {

    @Autowired
    private RiskCalculatorToolService riskCalculatorToolService;

    @Test
    void shouldCalculatePortfolioReturn() {
        Map<String, Object> result = riskCalculatorToolService.calculatePortfolioReturn(60, 30, 10);

        assertNotNull(result);
        assertEquals("60%", result.get("stockAllocation"));
        assertEquals("30%", result.get("bondAllocation"));
        assertEquals("10%", result.get("cashAllocation"));
        assertNotNull(result.get("expectedAnnualReturn"));
        assertNotNull(result.get("riskLevel"));
    }

    @Test
    void shouldRejectInvalidAllocation() {
        Map<String, Object> result = riskCalculatorToolService.calculatePortfolioReturn(50, 30, 10);

        assertNotNull(result);
        assertNotNull(result.get("error"));
        assertTrue(((String) result.get("error")).contains("100%"));
    }

    @Test
    void shouldCalculateValueAtRisk() {
        Map<String, Object> result = riskCalculatorToolService.calculateValueAtRisk(100000, 0.95, 30);

        assertNotNull(result);
        assertEquals(100000.0, result.get("investmentAmount"));
        assertEquals("95.0%", result.get("confidenceLevel"));
        assertEquals("30天", result.get("holdingPeriodDays"));
        assertNotNull(result.get("var"));
        assertNotNull(result.get("varPercent"));
    }

    @Test
    void shouldCalculateSharpeRatio() {
        Map<String, Object> result = riskCalculatorToolService.calculateSharpeRatio(0.12, 0.03, 0.20);

        assertNotNull(result);
        assertEquals("12.0%", result.get("portfolioReturn"));
        assertEquals("3.0%", result.get("riskFreeRate"));
        assertEquals("20.0%", result.get("portfolioVolatility"));
        assertNotNull(result.get("sharpeRatio"));
        assertNotNull(result.get("evaluation"));
    }

    @Test
    void shouldEvaluateSharpeRatio() {
        // 优秀 (sharpeRatio > 1.0)
        Map<String, Object> result1 = riskCalculatorToolService.calculateSharpeRatio(0.15, 0.03, 0.10);
        assertEquals("优秀", result1.get("evaluation"));

        // 良好 (0.5 < sharpeRatio <= 1.0)
        Map<String, Object> result2 = riskCalculatorToolService.calculateSharpeRatio(0.10, 0.03, 0.10);
        assertEquals("良好", result2.get("evaluation"));

        // 一般 (0 < sharpeRatio <= 0.5)
        Map<String, Object> result3 = riskCalculatorToolService.calculateSharpeRatio(0.06, 0.03, 0.10);
        assertEquals("一般", result3.get("evaluation"));
    }
}
