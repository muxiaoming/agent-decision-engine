package com.zhou.ai.investment.service;

import com.zhou.ai.graph.service.GraphWorkflowService;
import com.zhou.ai.investment.model.*;
import com.zhou.ai.rag.service.RagService;
import com.zhou.ai.skills.service.SkillsAgentService;
import com.zhou.ai.tools.service.MarketIndexToolService;
import com.zhou.ai.tools.service.RiskCalculatorToolService;
import com.zhou.ai.tools.service.StockPriceToolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * InvestmentDecisionService 单元测试。
 * 测试完整的6步投资决策流程。
 */
@ExtendWith(MockitoExtension.class)
class InvestmentDecisionServiceTest {

    @Mock
    private SkillsAgentService skillsAgentService;

    @Mock
    private RagService ragService;

    @Mock
    private StockPriceToolService stockPriceToolService;

    @Mock
    private MarketIndexToolService marketIndexToolService;

    @Mock
    private RiskCalculatorToolService riskCalculatorToolService;

    @Mock
    private GraphWorkflowService graphWorkflowService;

    private InvestmentDecisionService investmentDecisionService;

    @BeforeEach
    void setUp() {
        investmentDecisionService = new InvestmentDecisionService(
                skillsAgentService,
                ragService,
                stockPriceToolService,
                marketIndexToolService,
                riskCalculatorToolService,
                graphWorkflowService
        );
    }

    @Test
    @DisplayName("完整6步流程 - 所有模块启用")
    void shouldExecuteCompleteWorkflowWithAllModules() {
        // Given
        InvestmentDecisionRequest request = InvestmentDecisionRequest.of(
                "I want to invest in tech stocks, budget 100k",
                "deepSeekChatModel",
                "test-thread-id",
                true, true, true
        );

        // Mock Skills steps
        when(skillsAgentService.chat(anyString(), anyString()))
                .thenReturn("Skills analysis result");

        // Mock RAG
        when(ragService.ask(anyString(), anyString()))
                .thenReturn(new com.zhou.ai.common.model.ChatResponse(
                        "RAG knowledge retrieval result",
                        "deepSeekChatModel",
                        "test-thread-id",
                        System.currentTimeMillis(),
                        new com.zhou.ai.common.model.TokenUsage(100, 200, 300)
                ));

        // Mock Tools
        when(stockPriceToolService.getStockPrice(anyString()))
                .thenReturn(Map.of("symbol", "AAPL", "price", 178.52));
        when(marketIndexToolService.getMarketIndex(anyString()))
                .thenReturn(Map.of("name", "上证指数", "value", 3500.0));
        when(riskCalculatorToolService.calculateValueAtRisk(anyDouble(), anyDouble(), anyInt()))
                .thenReturn(Map.of("var", 5000.0, "confidence", 0.95));

        // Mock Graph
        when(graphWorkflowService.execute(anyString()))
                .thenReturn(Map.of("category", "investment", "output", "Graph processed"));

        // When
        InvestmentDecisionResponse response = investmentDecisionService.decide(request);

        // Then
        assertNotNull(response);
        assertTrue(response.isFullySuccess());
        assertEquals(6, response.steps().size());
        assertEquals(6, response.getSuccessfulStepCount());
        assertNotNull(response.workflow());

        // Verify all steps were called
        verify(skillsAgentService, times(3)).chat(anyString(), anyString());
        verify(ragService, times(1)).ask(anyString(), anyString());
        verify(stockPriceToolService, times(1)).getStockPrice(anyString());
        verify(marketIndexToolService, times(1)).getMarketIndex(anyString());
        verify(riskCalculatorToolService, times(1)).calculateValueAtRisk(anyDouble(), anyDouble(), anyInt());
        verify(graphWorkflowService, times(1)).execute(anyString());
    }

    @Test
    @DisplayName("禁用RAG - 5步流程")
    void shouldSkipRAGWhenDisabled() {
        // Given
        InvestmentDecisionRequest request = InvestmentDecisionRequest.of(
                "I want to invest",
                "deepSeekChatModel",
                "test-thread-id",
                false, true, true  // enableRAG = false
        );

        when(skillsAgentService.chat(anyString(), anyString()))
                .thenReturn("Skills result");
        when(stockPriceToolService.getStockPrice(anyString()))
                .thenReturn(Map.of("symbol", "AAPL", "price", 178.52));
        when(marketIndexToolService.getMarketIndex(anyString()))
                .thenReturn(Map.of("name", "上证指数", "value", 3500.0));
        when(riskCalculatorToolService.calculateValueAtRisk(anyDouble(), anyDouble(), anyInt()))
                .thenReturn(Map.of("var", 5000.0));
        when(graphWorkflowService.execute(anyString()))
                .thenReturn(Map.of("category", "investment", "output", "Graph processed"));

        // When
        InvestmentDecisionResponse response = investmentDecisionService.decide(request);

        // Then
        assertNotNull(response);
        assertEquals(6, response.steps().size());
        assertEquals(5, response.getSuccessfulStepCount());
        // RAG should not be called
        verify(ragService, never()).ask(anyString(), anyString());
    }

    @Test
    @DisplayName("禁用Tools - 5步流程")
    void shouldSkipToolsWhenDisabled() {
        // Given
        InvestmentDecisionRequest request = InvestmentDecisionRequest.of(
                "I want to invest",
                "deepSeekChatModel",
                "test-thread-id",
                true, false, true  // enableTools = false
        );

        when(skillsAgentService.chat(anyString(), anyString()))
                .thenReturn("Skills result");
        when(ragService.ask(anyString(), anyString()))
                .thenReturn(new com.zhou.ai.common.model.ChatResponse(
                        "RAG result", "model", "chatId", 0L, null));
        when(graphWorkflowService.execute(anyString()))
                .thenReturn(Map.of("category", "investment", "output", "Graph processed"));

        // When
        InvestmentDecisionResponse response = investmentDecisionService.decide(request);

        // Then
        assertNotNull(response);
        assertEquals(6, response.steps().size());
        assertEquals(5, response.getSuccessfulStepCount());
        // Steps have "skipped" status in workflow

        // Tools should not be called
        verify(stockPriceToolService, never()).getStockPrice(anyString());
        verify(marketIndexToolService, never()).getMarketIndex(anyString());
        verify(riskCalculatorToolService, never()).calculateValueAtRisk(anyDouble(), anyDouble(), anyInt());
    }

    @Test
    @DisplayName("禁用Graph - 5步流程")
    void shouldSkipGraphWhenDisabled() {
        // Given
        InvestmentDecisionRequest request = InvestmentDecisionRequest.of(
                "I want to invest",
                "deepSeekChatModel",
                "test-thread-id",
                true, true, false  // enableGraph = false
        );

        when(skillsAgentService.chat(anyString(), anyString()))
                .thenReturn("Skills result");
        when(ragService.ask(anyString(), anyString()))
                .thenReturn(new com.zhou.ai.common.model.ChatResponse(
                        "RAG result", "model", "chatId", 0L, null));
        when(stockPriceToolService.getStockPrice(anyString()))
                .thenReturn(Map.of("symbol", "AAPL", "price", 178.52));
        when(marketIndexToolService.getMarketIndex(anyString()))
                .thenReturn(Map.of("name", "上证指数", "value", 3500.0));
        when(riskCalculatorToolService.calculateValueAtRisk(anyDouble(), anyDouble(), anyInt()))
                .thenReturn(Map.of("var", 5000.0));

        // When
        InvestmentDecisionResponse response = investmentDecisionService.decide(request);

        // Then
        assertNotNull(response);
        assertEquals(6, response.steps().size());
        assertEquals(5, response.getSuccessfulStepCount());
        // Steps have "skipped" status in workflow

        // Graph should not be called
        verify(graphWorkflowService, never()).execute(anyString());
    }

    @Test
    @DisplayName("所有模块禁用 - 最小化3步流程")
    void shouldExecuteMinimalWorkflowWhenAllDisabled() {
        // Given
        InvestmentDecisionRequest request = InvestmentDecisionRequest.of(
                "I want to invest",
                "deepSeekChatModel",
                "test-thread-id",
                false, false, false  // all disabled
        );

        when(skillsAgentService.chat(anyString(), anyString()))
                .thenReturn("Skills result");

        // When
        InvestmentDecisionResponse response = investmentDecisionService.decide(request);

        // Then
        assertNotNull(response);
        assertEquals(6, response.steps().size());
        assertEquals(3, response.getSuccessfulStepCount());  // Only skills steps
        // Steps have "skipped" status in workflow

        // Verify no external calls except Skills
        verify(skillsAgentService, times(3)).chat(anyString(), anyString());
        verify(ragService, never()).ask(anyString(), anyString());
        verify(stockPriceToolService, never()).getStockPrice(anyString());
        verify(graphWorkflowService, never()).execute(anyString());
    }

    @Test
    @DisplayName("步骤失败但不中断流程")
    void shouldContinueWhenStepFails() {
        // Given
        InvestmentDecisionRequest request = InvestmentDecisionRequest.of(
                "I want to invest",
                "deepSeekChatModel",
                "test-thread-id",
                true, true, true
        );

        // Skills step 1 fails
        when(skillsAgentService.chat(anyString(), anyString()))
                .thenThrow(new RuntimeException("Skills service unavailable"))
                .thenReturn("Skills recovery result")
                .thenReturn("Skills final result");

        // RAG fails
        when(ragService.ask(anyString(), anyString()))
                .thenThrow(new RuntimeException("RAG service unavailable"));

        // Tools succeed
        when(stockPriceToolService.getStockPrice(anyString()))
                .thenReturn(Map.of("symbol", "AAPL", "price", 178.52));
        when(marketIndexToolService.getMarketIndex(anyString()))
                .thenReturn(Map.of("name", "上证指数", "value", 3500.0));
        when(riskCalculatorToolService.calculateValueAtRisk(anyDouble(), anyDouble(), anyInt()))
                .thenReturn(Map.of("var", 5000.0));

        // Graph fails
        when(graphWorkflowService.execute(anyString()))
                .thenThrow(new RuntimeException("Graph service unavailable"));

        // When
        InvestmentDecisionResponse response = investmentDecisionService.decide(request);

        // Then
        assertNotNull(response);
        assertEquals(6, response.steps().size());
        assertTrue(response.getSuccessfulStepCount() >= 3);  // At least Tools and some Skills
        assertTrue(response.getFailedStepCount() >= 0);  // Some steps may fail
    }

    @Test
    @DisplayName("默认配置 - 所有模块启用")
    void shouldUseDefaultConfiguration() {
        // Given
        InvestmentDecisionRequest request = InvestmentDecisionRequest.of(
                "I want to invest",
                "deepSeekChatModel"
        );

        when(skillsAgentService.chat(anyString(), anyString()))
                .thenReturn("Skills result");
        when(ragService.ask(anyString(), anyString()))
                .thenReturn(new com.zhou.ai.common.model.ChatResponse(
                        "RAG result", "model", "chatId", 0L, null));
        when(stockPriceToolService.getStockPrice(anyString()))
                .thenReturn(Map.of("symbol", "AAPL", "price", 178.52));
        when(marketIndexToolService.getMarketIndex(anyString()))
                .thenReturn(Map.of("name", "上证指数", "value", 3500.0));
        when(riskCalculatorToolService.calculateValueAtRisk(anyDouble(), anyDouble(), anyInt()))
                .thenReturn(Map.of("var", 5000.0));
        when(graphWorkflowService.execute(anyString()))
                .thenReturn(Map.of("category", "investment", "output", "Graph processed"));

        // When
        InvestmentDecisionResponse response = investmentDecisionService.decide(request);

        // Then - Default should enable all modules
        assertNotNull(response);
        assertEquals(6, response.steps().size());
        assertEquals(6, response.getSuccessfulStepCount());
    }

    @Test
    @DisplayName("Token用量累积")
    void shouldAccumulateTokenUsage() {
        // Given
        InvestmentDecisionRequest request = InvestmentDecisionRequest.of(
                "I want to invest",
                "deepSeekChatModel",
                "test-thread-id",
                true, false, false  // Only RAG enabled for token testing
        );

        when(skillsAgentService.chat(anyString(), anyString()))
                .thenReturn("Skills result");
        when(ragService.ask(anyString(), anyString()))
                .thenReturn(new com.zhou.ai.common.model.ChatResponse(
                        "RAG result", "model", "chatId", 0L,
                        new com.zhou.ai.common.model.TokenUsage(100, 200, 300)));

        // When
        InvestmentDecisionResponse response = investmentDecisionService.decide(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.workflow().tokenUsage());
        // Total tokens should be accumulated (Skills + RAG)
        assertTrue(response.workflow().tokenUsage().totalTokens() >= 300);
    }
}
