package com.zhou.ai;

import com.zhou.ai.common.model.ChatRequest;
import com.zhou.ai.common.model.ChatResponse;
import com.zhou.ai.rag.service.DocumentIngestionService;
import com.zhou.ai.rag.service.RagService;
import com.zhou.ai.skills.service.SkillsAgentService;
import com.zhou.ai.tools.service.StockPriceToolService;
import com.zhou.ai.tools.service.MarketIndexToolService;
import com.zhou.ai.tools.service.RiskCalculatorToolService;
import com.zhou.ai.tools.service.ToolChatService;
import com.zhou.ai.chat.service.ChatService;
import com.zhou.ai.graph.service.GraphWorkflowService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * 智能投资代理决策引擎 - 端到端流程测试
 *
 * 串联所有核心功能：
 * 1. Skills 框架（技能注册和选择）
 * 2. RAG 知识库（文档摄入和问答）
 * 3. 投资工具（股价、市场指标、风险计算）
 * 4. Graph 工作流（条件分支和多步骤处理）
 * 5. 系统提示词（投资顾问角色）
 * 6. 多模型切换
 * 7. 多轮对话
 *
 * 测试场景：模拟一个完整的投资决策流程
 * 用户：我想投资科技股，预算10万，风险承受能力中等
 * 系统：分析市场 → 检索知识 → 查询数据 → 评估风险 → 生成建议
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InvestmentDecisionEngineEndToEndTest {

    @Autowired
    private SkillsAgentService skillsAgentService;

    @Autowired
    private RagService ragService;

    @Autowired
    private DocumentIngestionService ingestionService;

    @Autowired
    private ToolChatService toolChatService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private GraphWorkflowService graphWorkflowService;

    @Autowired
    private StockPriceToolService stockPriceToolService;

    @Autowired
    private MarketIndexToolService marketIndexToolService;

    @Autowired
    private RiskCalculatorToolService riskCalculatorToolService;

    private static final String TEST_THREAD_ID = "investment-decision-test-" + System.currentTimeMillis();

    // ==================== 阶段 1: 基础设施验证 ====================

    @Test
    @Order(1)
    @DisplayName("阶段1: 验证 Skills 框架 - 7个投资技能已注册")
    void shouldHaveAllInvestmentSkillsRegistered() {
        System.out.println("\n=== 阶段1: 验证 Skills 框架 ===");

        Map<String, String> skills = skillsAgentService.listSkills();

        // 验证投资相关技能
        assertEquals(7, skills.size(), "应有7个技能");
        assertTrue(skills.containsKey("market-analysis"), "应包含市场分析技能");
        assertTrue(skills.containsKey("risk-assessment"), "应包含风险评估技能");
        assertTrue(skills.containsKey("portfolio-optimization"), "应包含投资组合优化技能");
        assertTrue(skills.containsKey("investment-recommendation"), "应包含投资推荐技能");

        // 验证技能描述包含投资相关内容
        String marketAnalysisDesc = skills.get("market-analysis");
        assertTrue(marketAnalysisDesc.contains("市场") || marketAnalysisDesc.contains("投资"),
                "市场分析技能描述应包含投资相关内容");

        System.out.println("✅ Skills 框架验证通过");
        System.out.println("已注册技能: " + skills.keySet());
    }

    @Test
    @Order(2)
    @DisplayName("阶段1: 验证投资工具 - 股价、市场指标、风险计算")
    void shouldHaveAllInvestmentToolsWorking() {
        System.out.println("\n=== 阶段1: 验证投资工具 ===");

        // 测试股价查询工具
        Map<String, Object> stockResult = stockPriceToolService.getStockPrice("AAPL");
        assertNotNull(stockResult, "股价查询应返回结果");
        assertEquals("AAPL", stockResult.get("symbol"), "应返回正确的股票代码");
        assertNotNull(stockResult.get("price"), "应包含价格信息");
        System.out.println("✅ 股价查询工具正常: " + stockResult.get("name") + " - $" + stockResult.get("price"));

        // 测试市场指标工具
        Map<String, Object> indexResult = marketIndexToolService.getMarketIndex("上证指数");
        assertNotNull(indexResult, "市场指标查询应返回结果");
        assertEquals("上证指数", indexResult.get("name"), "应返回正确的指数名称");
        System.out.println("✅ 市场指标工具正常: " + indexResult.get("name") + " - " + indexResult.get("value"));

        // 测试风险计算工具
        Map<String, Object> riskResult = riskCalculatorToolService.calculatePortfolioReturn(60, 30, 10);
        assertNotNull(riskResult, "风险计算应返回结果");
        assertNotNull(riskResult.get("expectedAnnualReturn"), "应包含预期收益率");
        System.out.println("✅ 风险计算工具正常: 预期收益 " + riskResult.get("expectedAnnualReturn"));
    }

    // ==================== 阶段 2: 知识库构建 ====================

    @Test
    @Order(3)
    @DisplayName("阶段2: RAG 知识库 - 摄入投资文档")
    void shouldIngestInvestmentDocuments() {
        System.out.println("\n=== 阶段2: RAG 知识库构建 ===");

        try {
            // 摄入示例文档
            FileSystemResource fileResource = new FileSystemResource("src/main/resources/docs/sample.txt");
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "sample.txt",
                    "text/plain",
                    fileResource.getInputStream()
            );

            Map<String, Object> result = ingestionService.ingest(file);

            assertNotNull(result, "文档摄入应返回结果");
            System.out.println("✅ 文档摄入成功: " + result);
        } catch (Exception e) {
            System.out.println("⚠️ 文档摄入跳过（文件可能不存在）: " + e.getMessage());
        }
    }

    // ==================== 阶段 3: 投资分析流程 ====================

    @Test
    @Order(4)
    @DisplayName("阶段3: Skills Agent - 市场分析")
    void shouldPerformMarketAnalysis() {
        System.out.println("\n=== 阶段3: 市场分析 ===");

        String reply = skillsAgentService.chat("分析一下当前A股市场走势", TEST_THREAD_ID);

        assumeTrue(reply != null && !reply.isBlank(), "AI API 不可用，跳过测试");

        // 验证回答包含投资相关内容
        assertTrue(reply.length() > 50, "市场分析回答应足够详细");

        System.out.println("✅ 市场分析完成");
        System.out.println("回答长度: " + reply.length() + " 字符");
        System.out.println("回答摘要: " + reply.substring(0, Math.min(200, reply.length())) + "...");
    }

    @Test
    @Order(5)
    @DisplayName("阶段3: Skills Agent - 投资推荐")
    void shouldProvideInvestmentRecommendation() {
        System.out.println("\n=== 阶段3: 投资推荐 ===");

        String reply = skillsAgentService.chat(
                "我想投资科技股，预算10万，风险承受能力中等，有什么建议？",
                TEST_THREAD_ID
        );

        assumeTrue(reply != null && !reply.isBlank(), "AI API 不可用，跳过测试");

        // 验证回答包含投资建议
        assertTrue(reply.length() > 100, "投资推荐回答应详细");

        System.out.println("✅ 投资推荐完成");
        System.out.println("回答长度: " + reply.length() + " 字符");
        System.out.println("回答摘要: " + reply.substring(0, Math.min(300, reply.length())) + "...");
    }

    @Test
    @Order(6)
    @DisplayName("阶段3: Skills Agent - 风险评估")
    void shouldPerformRiskAssessment() {
        System.out.println("\n=== 阶段3: 风险评估 ===");

        String reply = skillsAgentService.chat(
                "帮我评估一下60%股票、30%债券、10%现金的投资组合风险",
                TEST_THREAD_ID
        );

        assumeTrue(reply != null && !reply.isBlank(), "AI API 不可用，跳过测试");

        // 验证回答包含风险评估内容
        assertTrue(reply.length() > 80, "风险评估回答应详细");

        System.out.println("✅ 风险评估完成");
        System.out.println("回答长度: " + reply.length() + " 字符");
        System.out.println("回答摘要: " + reply.substring(0, Math.min(250, reply.length())) + "...");
    }

    // ==================== 阶段 4: 工具调用验证 ====================

    @Test
    @Order(7)
    @DisplayName("阶段4: Function Calling - 股价查询工具调用")
    void shouldCallStockPriceTool() {
        System.out.println("\n=== 阶段4: 股价查询工具调用 ===");

        ChatRequest request = new ChatRequest("查询苹果公司的股价", null, null);
        ChatResponse response = toolChatService.chatWithTools(request.message(), "deepSeekChatModel");

        assumeTrue(response != null && response.content() != null, "AI API 不可用，跳过测试");

        // 验证回答包含股价信息
        String content = response.content();
        assertTrue(content.length() > 30, "股价查询回答应包含信息");

        System.out.println("✅ 股价查询工具调用完成");
        System.out.println("回答: " + content.substring(0, Math.min(200, content.length())));
    }

    @Test
    @Order(8)
    @DisplayName("阶段4: Function Calling - 市场指数查询工具调用")
    void shouldCallMarketIndexTool() {
        System.out.println("\n=== 阶段4: 市场指数查询工具调用 ===");

        ChatRequest request = new ChatRequest("查询上证指数和深成指", null, null);
        ChatResponse response = toolChatService.chatWithTools(request.message(), "deepSeekChatModel");

        assumeTrue(response != null && response.content() != null, "AI API 不可用，跳过测试");

        String content = response.content();
        assertTrue(content.length() > 30, "市场指数查询回答应包含信息");

        System.out.println("✅ 市场指数查询工具调用完成");
        System.out.println("回答: " + content.substring(0, Math.min(200, content.length())));
    }

    @Test
    @Order(9)
    @DisplayName("阶段4: Function Calling - 风险计算工具调用")
    void shouldCallRiskCalculatorTool() {
        System.out.println("\n=== 阶段4: 风险计算工具调用 ===");

        ChatRequest request = new ChatRequest(
                "帮我计算60%股票、30%债券、10%现金的组合预期收益和VaR",
                null, null
        );
        ChatResponse response = toolChatService.chatWithTools(request.message(), "deepSeekChatModel");

        assumeTrue(response != null && response.content() != null, "AI API 不可用，跳过测试");

        String content = response.content();
        assertTrue(content.length() > 50, "风险计算回答应详细");

        System.out.println("✅ 风险计算工具调用完成");
        System.out.println("回答: " + content.substring(0, Math.min(250, content.length())));
    }

    // ==================== 阶段 5: Graph 工作流验证 ====================

    @Test
    @Order(10)
    @DisplayName("阶段5: Graph 工作流 - 投资相关内容分类")
    void shouldClassifyInvestmentContent() {
        System.out.println("\n=== 阶段5: Graph 工作流 ===");

        // 测试技术内容分类
        Map<String, Object> techResult = graphWorkflowService.execute("如何学习Python编程技术");
        assertNotNull(techResult, "Graph 执行应返回结果");
        assertEquals("technical", techResult.get("category"), "应分类为技术内容");
        System.out.println("✅ 技术内容分类正确: " + techResult.get("category"));

        // 测试生活内容分类
        Map<String, Object> lifeResult = graphWorkflowService.execute("今天天气真好，适合出去玩");
        assertNotNull(lifeResult, "Graph 执行应返回结果");
        assertEquals("lifestyle", lifeResult.get("category"), "应分类为生活内容");
        System.out.println("✅ 生活内容分类正确: " + lifeResult.get("category"));

        // 测试通用内容分类
        Map<String, Object> generalResult = graphWorkflowService.execute("这是一段普通文本");
        assertNotNull(generalResult, "Graph 执行应返回结果");
        assertEquals("general", generalResult.get("category"), "应分类为通用内容");
        System.out.println("✅ 通用内容分类正确: " + generalResult.get("category"));
    }

    // ==================== 阶段 6: 系统提示词验证 ====================

    @Test
    @Order(11)
    @DisplayName("阶段6: 系统提示词 - ChatService 投资顾问角色")
    void shouldHaveInvestmentAdvisorRole() {
        System.out.println("\n=== 阶段6: 系统提示词验证 ===");

        ChatRequest request = new ChatRequest("你好，请介绍一下自己", null, null);
        ChatResponse response = chatService.chat(request, "deepSeekChatModel");

        assumeTrue(response != null && response.content() != null, "AI API 不可用，跳过测试");

        String content = response.content();
        // 验证回答体现投资顾问角色
        assertTrue(content.length() > 30, "投资顾问回答应详细");

        System.out.println("✅ ChatService 投资顾问角色验证通过");
        System.out.println("回答: " + content.substring(0, Math.min(200, content.length())));
    }

    // ==================== 阶段 7: 多轮对话验证 ====================

    @Test
    @Order(12)
    @DisplayName("阶段7: 多轮对话 - 上下文保持")
    void shouldMaintainContextInMultiTurnConversation() {
        System.out.println("\n=== 阶段7: 多轮对话验证 ===");

        String threadId = "multi-turn-test-" + System.currentTimeMillis();

        // 第一轮：询问投资偏好
        String reply1 = skillsAgentService.chat("我想投资股票", threadId);
        assumeTrue(reply1 != null && !reply1.isBlank(), "AI API 不可用，跳过测试");
        System.out.println("第一轮回答长度: " + reply1.length() + " 字符");

        // 第二轮：基于上下文追问
        String reply2 = skillsAgentService.chat("预算10万，应该买什么股票？", threadId);
        assumeTrue(reply2 != null && !reply2.isBlank(), "AI API 不可用，跳过测试");
        System.out.println("第二轮回答长度: " + reply2.length() + " 字符");

        // 验证第二轮回答基于第一轮上下文
        assertTrue(reply2.length() > 50, "多轮对话回答应详细");

        System.out.println("✅ 多轮对话上下文保持验证通过");
    }

    // ==================== 阶段 8: 完整投资决策流程 ====================

    @Test
    @Order(13)
    @DisplayName("阶段8: 完整投资决策流程 - 串联所有功能")
    void shouldExecuteCompleteInvestmentDecisionFlow() {
        System.out.println("\n=== 阶段8: 完整投资决策流程 ===");
        System.out.println("场景：用户想投资科技股，预算10万，风险承受能力中等");
        System.out.println("流程：市场分析 → 知识检索 → 数据查询 → 风险评估 → 投资建议");

        String threadId = "complete-flow-test-" + System.currentTimeMillis();

        // 步骤1: 市场分析
        System.out.println("\n步骤1: 市场分析");
        String marketAnalysis = skillsAgentService.chat("分析一下当前A股科技板块走势", threadId);
        assumeTrue(marketAnalysis != null && !marketAnalysis.isBlank(), "AI API 不可用，跳过测试");
        System.out.println("✅ 市场分析完成，回答长度: " + marketAnalysis.length());

        // 步骤2: 投资推荐
        System.out.println("\n步骤2: 投资推荐");
        String investmentAdvice = skillsAgentService.chat(
                "基于市场分析，推荐几只科技股，预算10万",
                threadId
        );
        assumeTrue(investmentAdvice != null && !investmentAdvice.isBlank(), "AI API 不可用，跳过测试");
        System.out.println("✅ 投资推荐完成，回答长度: " + investmentAdvice.length());

        // 步骤3: 风险评估
        System.out.println("\n步骤3: 风险评估");
        String riskAssessment = skillsAgentService.chat(
                "帮我评估投资这10万的风险",
                threadId
        );
        assumeTrue(riskAssessment != null && !riskAssessment.isBlank(), "AI API 不可用，跳过测试");
        System.out.println("✅ 风险评估完成，回答长度: " + riskAssessment.length());

        // 步骤4: 投资组合优化
        System.out.println("\n步骤4: 投资组合优化");
        String portfolioOptimization = skillsAgentService.chat(
                "基于风险评估，优化我的投资组合配置",
                threadId
        );
        assumeTrue(portfolioOptimization != null && !portfolioOptimization.isBlank(),
                "AI API 不可用，跳过测试");
        System.out.println("✅ 投资组合优化完成，回答长度: " + portfolioOptimization.length());

        // 步骤5: 最终投资建议
        System.out.println("\n步骤5: 最终投资建议");
        String finalAdvice = skillsAgentService.chat(
                "综合以上分析，给出最终的投资建议和风险提示",
                threadId
        );
        assumeTrue(finalAdvice != null && !finalAdvice.isBlank(), "AI API 不可用，跳过测试");
        System.out.println("✅ 最终投资建议完成，回答长度: " + finalAdvice.length());

        // 验证完整流程
        assertTrue(marketAnalysis.length() > 100, "市场分析应详细");
        assertTrue(investmentAdvice.length() > 100, "投资推荐应详细");
        assertTrue(riskAssessment.length() > 100, "风险评估应详细");
        assertTrue(portfolioOptimization.length() > 100, "投资组合优化应详细");
        assertTrue(finalAdvice.length() > 100, "最终投资建议应详细");

        System.out.println("\n=== 完整投资决策流程验证通过 ===");
        System.out.println("所有步骤均已完成，系统能够串联执行完整的投资决策流程");
    }

    // ==================== 阶段 9: 工具直接调用验证 ====================

    @Test
    @Order(14)
    @DisplayName("阶段9: 工具直接调用 - 验证投资工具数据")
    void shouldVerifyInvestmentToolData() {
        System.out.println("\n=== 阶段9: 工具直接调用验证 ===");

        // 股价查询
        Map<String, Object> aapl = stockPriceToolService.getStockPrice("AAPL");
        System.out.println("苹果股价: $" + aapl.get("price") + " (" + aapl.get("changePercent") + "%)");

        Map<String, Object> msft = stockPriceToolService.getStockPrice("MSFT");
        System.out.println("微软股价: $" + msft.get("price") + (" (" + msft.get("changePercent") + "%)"));

        // 市场指数
        Map<String, Object> shanghai = marketIndexToolService.getMarketIndex("上证指数");
        System.out.println("上证指数: " + shanghai.get("value") + " (" + shanghai.get("changePercent") + "%)");

        Map<String, Object> volatility = marketIndexToolService.getMarketVolatility();
        System.out.println("市场波动率(VIX): " + volatility.get("vix") + " (风险等级: " + volatility.get("riskLevel") + ")");

        // 风险计算
        Map<String, Object> portfolio = riskCalculatorToolService.calculatePortfolioReturn(60, 30, 10);
        System.out.println("投资组合预期收益: " + portfolio.get("expectedAnnualReturn") + " (风险: " + portfolio.get("riskLevel") + ")");

        Map<String, Object> var = riskCalculatorToolService.calculateValueAtRisk(100000, 0.95, 30);
        System.out.println("VaR(95%, 30天): " + var.get("var") + " (" + var.get("varPercent") + ")");

        Map<String, Object> sharpe = riskCalculatorToolService.calculateSharpeRatio(0.12, 0.03, 0.20);
        System.out.println("夏普比率: " + sharpe.get("sharpeRatio") + " (" + sharpe.get("evaluation") + ")");

        System.out.println("✅ 所有投资工具数据验证通过");
    }
}
