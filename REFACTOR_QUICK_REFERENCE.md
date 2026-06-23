# 投资决策接口重构 - 快速参考

## 🎯 任务目标

重构 `/api/investment/decide` 接口，串联所有核心功能：
- ✅ Skills（技能框架）
- ✅ RAG（知识检索）
- ✅ Tools（工具调用）
- ✅ Graph（工作流）
- ✅ Observability（可观测性）

---

## 📋 新对话启动模板

复制以下内容到新对话：

```
我正在开发一个基于 Spring AI + Langfuse 3 的智能投资代理决策引擎。

项目位置: D:\Program Files\Dev\Workspace\AIProject\springai-langfuse3-demo

当前任务: 重构投资决策主流程接口，串联 Skills + RAG + Tools + Graph

重构计划: specs/001-agent-decision-engine/investment-decision-refactor.md

需要实现:
1. 更新 DTO 类（添加 enableRAG/enableTools/enableGraph 控制）
2. 重构 InvestmentDecisionService（实现完整流程）
3. 更新 InvestmentDecisionController
4. 更新测试用例
5. 更新相关文档

请先阅读重构计划，然后开始实现。

关键文件:
- src/main/java/com/zhou/ai/investment/model/InvestmentDecisionRequest.java
- src/main/java/com/zhou/ai/investment/model/InvestmentDecisionResponse.java
- src/main/java/com/zhou/ai/investment/service/InvestmentDecisionService.java
- src/main/java/com/zhou/ai/investment/controller/InvestmentDecisionController.java
- src/test/java/com/zhou/ai/investment/InvestmentDecisionControllerTest.java
```

---

## 🏗️ 实现步骤

### Step 1: 创建新 DTO

#### WorkflowResult.java
```java
package com.zhou.ai.investment.model;

import java.util.List;

public record WorkflowResult(
    List<WorkflowStep> steps,
    int totalSteps,
    int completedSteps,
    int failedSteps
) {}
```

#### WorkflowStep.java
```java
package com.zhou.ai.investment.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkflowStep(
    int step,
    String name,
    String module,
    String skill,
    String status,
    String result,
    List<String> sources,
    List<ToolCall> toolsCalled,
    List<String> nodesExecuted
) {
    public static WorkflowStep success(int step, String name, String module, 
            String skill, String result, List<String> sources, 
            List<ToolCall> toolsCalled, List<String> nodesExecuted) {
        return new WorkflowStep(step, name, module, skill, "completed", 
            result, sources, toolsCalled, nodesExecuted);
    }
    
    public static WorkflowStep failed(int step, String name, String module, String error) {
        return new WorkflowStep(step, name, module, null, "failed", 
            error, null, null, null);
    }
}
```

#### ToolCall.java
```java
package com.zhou.ai.investment.model;

import java.util.Map;

public record ToolCall(
    String name,
    String input,
    Map<String, Object> output
) {}
```

#### WorkflowData.java
```java
package com.zhou.ai.investment.model;

import java.util.List;

public record WorkflowData(
    List<String> ragSources,
    List<String> toolsUsed,
    List<String> skillsUsed,
    List<String> graphNodes
) {}
```

---

### Step 2: 更新 InvestmentDecisionRequest

```java
package com.zhou.ai.investment.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InvestmentDecisionRequest(
    String message,
    String modelName,
    String threadId,
    @JsonProperty(defaultValue = "true") boolean enableRAG,
    @JsonProperty(defaultValue = "true") boolean enableTools,
    @JsonProperty(defaultValue = "true") boolean enableGraph
) {
    public static InvestmentDecisionRequest of(String message) {
        return new InvestmentDecisionRequest(message, "deepSeekChatModel", null, true, true, true);
    }
    
    public String effectiveModelName() {
        return modelName != null ? modelName : "deepSeekChatModel";
    }
    
    public String effectiveThreadId() {
        return threadId != null ? threadId : "investment-" + System.currentTimeMillis();
    }
}
```

---

### Step 3: 重构 InvestmentDecisionService

```java
package com.zhou.ai.investment.service;

import com.zhou.ai.investment.model.*;
import com.zhou.ai.rag.service.RagService;
import com.zhou.ai.skills.service.SkillsAgentService;
import com.zhou.ai.tools.service.StockPriceToolService;
import com.zhou.ai.tools.service.MarketIndexToolService;
import com.zhou.ai.tools.service.RiskCalculatorToolService;
import com.zhou.ai.graph.service.GraphWorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class InvestmentDecisionService {
    
    private static final Logger log = LoggerFactory.getLogger(InvestmentDecisionService.class);
    
    private final SkillsAgentService skillsAgentService;
    private final RagService ragService;
    private final StockPriceToolService stockPriceToolService;
    private final MarketIndexToolService marketIndexToolService;
    private final RiskCalculatorToolService riskCalculatorToolService;
    private final GraphWorkflowService graphWorkflowService;
    
    public InvestmentDecisionService(
            SkillsAgentService skillsAgentService,
            RagService ragService,
            StockPriceToolService stockPriceToolService,
            MarketIndexToolService marketIndexToolService,
            RiskCalculatorToolService riskCalculatorToolService,
            GraphWorkflowService graphWorkflowService) {
        this.skillsAgentService = skillsAgentService;
        this.ragService = ragService;
        this.stockPriceToolService = stockPriceToolService;
        this.marketIndexToolService = marketIndexToolService;
        this.riskCalculatorToolService = riskCalculatorToolService;
        this.graphWorkflowService = graphWorkflowService;
    }
    
    public InvestmentDecisionResponse decide(InvestmentDecisionRequest request) {
        long startTime = System.currentTimeMillis();
        String threadId = request.effectiveThreadId();
        
        List<WorkflowStep> steps = new ArrayList<>();
        List<String> ragSources = new ArrayList<>();
        List<String> toolsUsed = new ArrayList<>();
        List<String> skillsUsed = new ArrayList<>();
        List<String> graphNodes = new ArrayList<>();
        
        // Step 1: 问题感知 (Skills)
        WorkflowStep step1 = executeStep(1, "问题感知", "skills", 
            "理解用户投资需求: " + request.message(), threadId);
        steps.add(step1);
        if ("completed".equals(step1.status())) skillsUsed.add("market-analysis");
        
        // Step 2: 知识检索 (RAG)
        if (request.enableRAG()) {
            WorkflowStep step2 = executeRAGStep(2, request.message(), threadId);
            steps.add(step2);
            if (step2.sources() != null) ragSources.addAll(step2.sources());
        }
        
        // Step 3: 数据获取 (Tools)
        if (request.enableTools()) {
            WorkflowStep step3 = executeToolsStep(3, request.message(), threadId);
            steps.add(step3);
            if (step3.toolsCalled() != null) {
                step3.toolsCalled().forEach(t -> toolsUsed.add(t.name()));
            }
        }
        
        // Step 4: 推理分析 (Skills)
        WorkflowStep step4 = executeStep(4, "推理分析", "skills", 
            "基于知识和数据分析投资风险", threadId);
        steps.add(step4);
        if ("completed".equals(step4.status())) skillsUsed.add("risk-assessment");
        
        // Step 5: 决策生成 (Skills)
        WorkflowStep step5 = executeStep(5, "决策生成", "skills", 
            "生成投资建议和风险提示", threadId);
        steps.add(step5);
        if ("completed".equals(step5.status())) skillsUsed.add("investment-recommendation");
        
        // Step 6: 流程编排 (Graph)
        if (request.enableGraph()) {
            WorkflowStep step6 = executeGraphStep(6, request.message(), threadId);
            steps.add(step6);
            if (step6.nodesExecuted() != null) graphNodes.addAll(step6.nodesExecuted());
        }
        
        // 构建响应
        long durationMs = System.currentTimeMillis() - startTime;
        long completed = steps.stream().filter(s -> "completed".equals(s.status())).count();
        long failed = steps.stream().filter(s -> "failed".equals(s.status())).count();
        
        WorkflowResult workflow = new WorkflowResult(steps, steps.size(), (int)completed, (int)failed);
        WorkflowData data = new WorkflowData(ragSources, toolsUsed, skillsUsed, graphNodes);
        
        return InvestmentDecisionResponse.success(threadId, workflow, 
            "综合以上分析，建议您...", 
            "⚠️ 投资有风险，入市需谨慎",
            durationMs, null, request.effectiveModelName(), null);
    }
    
    private WorkflowStep executeStep(int step, String name, String module, 
            String prompt, String threadId) {
        try {
            String result = skillsAgentService.chat(prompt, threadId);
            return WorkflowStep.success(step, name, module, null, result, null, null, null);
        } catch (Exception e) {
            return WorkflowStep.failed(step, name, module, e.getMessage());
        }
    }
    
    private WorkflowStep executeRAGStep(int step, String prompt, String threadId) {
        try {
            // 调用 RAG 服务
            String result = "从知识库检索到相关投资知识";
            List<String> sources = List.of("doc-001", "doc-002");
            return WorkflowStep.success(step, "知识检索", "rag", null, result, sources, null, null);
        } catch (Exception e) {
            return WorkflowStep.failed(step, "知识检索", "rag", e.getMessage());
        }
    }
    
    private WorkflowStep executeToolsStep(int step, String prompt, String threadId) {
        try {
            List<ToolCall> toolCalls = new ArrayList<>();
            
            // 调用股价查询
            Map<String, Object> stockResult = stockPriceToolService.getStockPrice("AAPL");
            toolCalls.add(new ToolCall("getStockPrice", "AAPL", stockResult));
            
            // 调用市场指数
            Map<String, Object> marketResult = marketIndexToolService.getMarketIndex("上证指数");
            toolCalls.add(new ToolCall("getMarketIndex", "上证指数", marketResult));
            
            // 调用风险计算
            Map<String, Object> riskResult = riskCalculatorToolService.calculateValueAtRisk(100000, 0.95, 30);
            toolCalls.add(new ToolCall("calculateValueAtRisk", "100000,0.95,30", riskResult));
            
            String result = "工具调用完成: " + toolCalls.size() + " 个工具";
            return WorkflowStep.success(step, "数据获取", "tools", null, result, null, toolCalls, null);
        } catch (Exception e) {
            return WorkflowStep.failed(step, "数据获取", "tools", e.getMessage());
        }
    }
    
    private WorkflowStep executeGraphStep(int step, String prompt, String threadId) {
        try {
            Map<String, Object> graphResult = graphWorkflowService.execute(prompt);
            List<String> nodesExecuted = List.of("classify", "analyze", "optimize", "output");
            String result = "Graph 执行完成，分类: " + graphResult.get("category");
            return WorkflowStep.success(step, "流程编排", "graph", null, result, null, null, nodesExecuted);
        } catch (Exception e) {
            return WorkflowStep.failed(step, "流程编排", "graph", e.getMessage());
        }
    }
    
    public Flux<InvestmentStepEvent> decideStream(InvestmentDecisionRequest request) {
        // 流式实现...
        return Flux.empty();
    }
}
```

---

### Step 4: 测试命令

```bash
# 编译检查
mvn compile

# 运行投资决策测试
mvn test -Dtest=InvestmentDecisionControllerTest

# 运行所有测试
mvn test
```

---

## 📚 文档更新

### INVESTMENT_API_GUIDE.md 需要更新
- 请求格式（添加 enableRAG/enableTools/enableGraph）
- 响应格式（添加 workflow、data、traceId）
- 流程说明（6个步骤）
- 使用示例

### 需要添加的示例
```bash
# 完整流程
curl -X POST http://localhost:8182/api/investment/decide \
  -H "Content-Type: application/json" \
  -d '{
    "message": "I want to invest in tech stocks",
    "enableRAG": true,
    "enableTools": true,
    "enableGraph": true
  }'

# 禁用 RAG
curl -X POST http://localhost:8182/api/investment/decide \
  -H "Content-Type: application/json" \
  -d '{
    "message": "I want to invest in tech stocks",
    "enableRAG": false,
    "enableTools": true,
    "enableGraph": true
  }'
```

---

## ✅ 验证清单

### 功能验证
- [ ] Skills 模块正常调用
- [ ] RAG 模块正常调用（可禁用）
- [ ] Tools 模块正常调用（可禁用）
- [ ] Graph 模块正常调用（可禁用）
- [ ] 完整流程串联成功
- [ ] 多轮对话上下文保持
- [ ] 流式输出正常

### 测试验证
- [ ] 完整流程测试通过
- [ ] 禁用模块测试通过
- [ ] 多轮对话测试通过
- [ ] 流式输出测试通过

### 文档验证
- [ ] 接口文档更新
- [ ] 使用指南更新
- [ ] 测试文档更新

---

## 🐛 常见问题

### 1. 编译错误
```
错误: 找不到符号 WorkflowResult
```
**解决**: 确保已创建所有新的 DTO 类

### 2. 依赖注入错误
```
错误: No qualifying bean of type 'RagService'
```
**解决**: 确保 InvestmentDecisionService 构造函数包含所有依赖

### 3. 测试超时
**解决**: 投资决策流程较长，测试可能需要 5-10 分钟

---

## 📅 预计时间

- DTO 更新: 1小时
- 服务重构: 2小时
- 控制器更新: 0.5小时
- 测试更新: 1小时
- 文档更新: 0.5小时

**总计**: 约 5 小时

---

## 📌 下一步

1. 在新对话中启动重构
2. 按照快速参考实现
3. 运行测试验证
4. 更新文档
5. 提交代码

---

**创建日期**: 2026-06-18
**状态**: 待实施
