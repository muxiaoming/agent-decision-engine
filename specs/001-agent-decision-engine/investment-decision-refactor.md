# 投资决策主流程接口 - 重构计划

**创建日期**: 2026-06-18
**状态**: 待实施
**优先级**: 高

---

## 🎯 重构目标

创建一个**真正串联所有核心功能**的接口，实现完整的投资决策流程：

```
Skills + RAG + Tools + Graph + Observability
```

---

## 📋 当前问题

### 现有设计缺陷
1. **只有 Skills 调用**：缺少 RAG、Tools、Graph
2. **流程不完整**：没有知识检索、数据获取、流程编排
3. **功能未串联**：各模块独立运行，未形成完整链路

### 需要重构的文件
- `InvestmentDecisionRequest.java` - 添加模块启用控制
- `InvestmentDecisionResponse.java` - 添加完整工作流信息
- `InvestmentDecisionService.java` - 实现完整决策流程
- `InvestmentDecisionController.java` - 更新接口定义
- `InvestmentDecisionControllerTest.java` - 更新测试用例

---

## 🏗️ 重构设计

### 1. 请求设计

```java
public record InvestmentDecisionRequest(
    String message,
    String modelName,
    String threadId,
    @JsonProperty(defaultValue = "true") boolean enableRAG,
    @JsonProperty(defaultValue = "true") boolean enableTools,
    @JsonProperty(defaultValue = "true") boolean enableGraph
) {}
```

### 2. 响应设计

```java
public record InvestmentDecisionResponse(
    String status,
    String threadId,
    WorkflowResult workflow,      // 工作流结果
    String finalAdvice,          // 最终建议
    String riskWarning,          // 风险提示
    WorkflowData data,           // 模块数据
    long durationMs,
    TokenUsage tokenUsage,
    String model,
    String traceId               // Langfuse 追踪ID
) {}
```

### 3. 完整决策流程

```
Step 1: 问题感知 (Skills)
  - 理解用户需求
  - 选择合适的技能
  ↓
Step 2: 知识检索 (RAG)
  - 检索财报、市场报告
  - 获取相关投资知识
  ↓
Step 3: 数据获取 (Tools)
  - 查询股价 (getStockPrice)
  - 查询市场指数 (getMarketIndex)
  - 计算风险 (calculateValueAtRisk)
  ↓
Step 4: 推理分析 (Skills)
  - 基于知识和数据分析
  - 评估投资风险
  ↓
Step 5: 决策生成 (Skills)
  - 生成投资建议
  - 包含风险提示
  ↓
Step 6: 流程编排 (Graph)
  - 执行决策流程
  - 验证决策完整性
```

---

## 📝 实现步骤

### 步骤 1: 更新 DTO 类

#### 1.1 更新 InvestmentDecisionRequest
```java
// 添加字段
boolean enableRAG;      // 是否启用RAG知识检索
boolean enableTools;    // 是否启用工具调用
boolean enableGraph;    // 是否启用Graph工作流
```

#### 1.2 创建 WorkflowResult
```java
public record WorkflowResult(
    List<WorkflowStep> steps,
    int totalSteps,
    int completedSteps,
    int failedSteps
) {}
```

#### 1.3 创建 WorkflowStep
```java
public record WorkflowStep(
    int step,
    String name,
    String module,           // "skills", "rag", "tools", "graph"
    String skill,
    String status,
    String result,
    List<String> sources,    // RAG 来源
    List<ToolCall> toolsCalled,  // 工具调用
    List<String> nodesExecuted   // Graph 节点
) {}
```

#### 1.4 创建 ToolCall
```java
public record ToolCall(
    String name,
    String input,
    Map<String, Object> output
) {}
```

#### 1.5 创建 WorkflowData
```java
public record WorkflowData(
    List<String> ragSources,
    List<String> toolsUsed,
    List<String> skillsUsed,
    List<String> graphNodes
) {}
```

---

### 步骤 2: 重构 InvestmentDecisionService

#### 2.1 核心方法
```java
public InvestmentDecisionResponse decide(InvestmentDecisionRequest request) {
    long startTime = System.currentTimeMillis();
    String threadId = request.effectiveThreadId();
    
    List<WorkflowStep> steps = new ArrayList<>();
    WorkflowData data = new WorkflowData();
    
    // Step 1: 问题感知 (Skills)
    if (true) {  // 始终执行
        WorkflowStep step1 = executeSkillsStep(1, "问题感知", ...);
        steps.add(step1);
    }
    
    // Step 2: 知识检索 (RAG)
    if (request.enableRAG()) {
        WorkflowStep step2 = executeRAGStep(2, "知识检索", ...);
        steps.add(step2);
    }
    
    // Step 3: 数据获取 (Tools)
    if (request.enableTools()) {
        WorkflowStep step3 = executeToolsStep(3, "数据获取", ...);
        steps.add(step3);
    }
    
    // Step 4: 推理分析 (Skills)
    if (true) {  // 始终执行
        WorkflowStep step4 = executeSkillsStep(4, "推理分析", ...);
        steps.add(step4);
    }
    
    // Step 5: 决策生成 (Skills)
    if (true) {  // 始终执行
        WorkflowStep step5 = executeSkillsStep(5, "决策生成", ...);
        steps.add(step5);
    }
    
    // Step 6: 流程编排 (Graph)
    if (request.enableGraph()) {
        WorkflowStep step6 = executeGraphStep(6, "流程编排", ...);
        steps.add(step6);
    }
    
    // 构建响应
    WorkflowResult workflow = new WorkflowResult(steps, steps.size(), 
        steps.stream().filter(s -> "completed".equals(s.status())).count(),
        steps.stream().filter(s -> "failed".equals(s.status())).count());
    
    return InvestmentDecisionResponse.success(threadId, workflow, ...);
}
```

#### 2.2 实现各模块调用

##### RAG 调用
```java
private WorkflowStep executeRAGStep(int step, String name, String prompt, String threadId) {
    try {
        ChatResponse ragResponse = ragService.ask(prompt, "deepSeekChatModel");
        String result = ragResponse.content();
        List<String> sources = ragResponse.sources();  // 需要扩展 ChatResponse
        
        return WorkflowStep.success(step, name, "rag", null, result, sources, null, null);
    } catch (Exception e) {
        return WorkflowStep.failed(step, name, "rag", e.getMessage());
    }
}
```

##### Tools 调用
```java
private WorkflowStep executeToolsStep(int step, String name, String userMessage, String threadId) {
    try {
        List<ToolCall> toolCalls = new ArrayList<>();
        
        // 调用股价查询工具
        if (userMessage.contains("股票") || userMessage.contains("股价")) {
            Map<String, Object> stockResult = stockPriceToolService.getStockPrice("AAPL");
            toolCalls.add(new ToolCall("getStockPrice", "AAPL", stockResult));
        }
        
        // 调用市场指数工具
        if (userMessage.contains("市场") || userMessage.contains("指数")) {
            Map<String, Object> marketResult = marketIndexToolService.getMarketIndex("上证指数");
            toolCalls.add(new ToolCall("getMarketIndex", "上证指数", marketResult));
        }
        
        // 调用风险计算工具
        Map<String, Object> riskResult = riskCalculatorToolService.calculateValueAtRisk(100000, 0.95, 30);
        toolCalls.add(new ToolCall("calculateValueAtRisk", "100000,0.95,30", riskResult));
        
        String result = "工具调用完成: " + toolCalls.size() + " 个工具";
        
        return WorkflowStep.success(step, name, "tools", null, result, null, toolCalls, null);
    } catch (Exception e) {
        return WorkflowStep.failed(step, name, "tools", e.getMessage());
    }
}
```

##### Graph 调用
```java
private WorkflowStep executeGraphStep(int step, String name, String input, String threadId) {
    try {
        Map<String, Object> graphResult = graphWorkflowService.execute(input);
        List<String> nodesExecuted = List.of("classify", "analyze", "optimize", "output");
        
        String result = "Graph 执行完成，分类: " + graphResult.get("category");
        
        return WorkflowStep.success(step, name, "graph", null, result, null, null, nodesExecuted);
    } catch (Exception e) {
        return WorkflowStep.failed(step, name, "graph", e.getMessage());
    }
}
```

---

### 步骤 3: 更新控制器

#### 3.1 同步端点
```java
@PostMapping("/decide")
public ResponseEntity<InvestmentDecisionResponse> decide(
        @RequestBody InvestmentDecisionRequest request) {
    InvestmentDecisionResponse response = investmentDecisionService.decide(request);
    return ResponseEntity.ok(response);
}
```

#### 3.2 流式端点
```java
@PostMapping(value = "/decide/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<InvestmentStepEvent> decideStream(
        @RequestBody InvestmentDecisionRequest request) {
    return investmentDecisionService.decideStream(request);
}
```

---

### 步骤 4: 更新测试

#### 4.1 完整流程测试
```java
@Test
void shouldExecuteCompleteWorkflow() {
    InvestmentDecisionRequest request = InvestmentDecisionRequest.of(
        "I want to invest in tech stocks, budget 100k",
        "deepSeekChatModel"
    );
    request = request.withEnableRAG(true).withEnableTools(true).withEnableGraph(true);
    
    InvestmentDecisionResponse response = service.decide(request);
    
    assertEquals("success", response.status());
    assertEquals(6, response.workflow().totalSteps());
    assertEquals(6, response.workflow().completedSteps());
    assertNotNull(response.data().ragSources());
    assertNotNull(response.data().toolsUsed());
    assertNotNull(response.data().graphNodes());
}
```

#### 4.2 禁用模块测试
```java
@Test
void shouldSkipRAGWhenDisabled() {
    InvestmentDecisionRequest request = InvestmentDecisionRequest.of(
        "I want to invest",
        "deepSeekChatModel"
    );
    request = request.withEnableRAG(false);
    
    InvestmentDecisionResponse response = service.decide(request);
    
    assertEquals(5, response.workflow().totalSteps());  // 跳过RAG步骤
    assertNull(response.data().ragSources());
}
```

---

## 📊 文件变更清单

### 新增文件
- `WorkflowResult.java` - 工作流结果
- `WorkflowStep.java` - 工作流步骤
- `ToolCall.java` - 工具调用记录
- `WorkflowData.java` - 模块数据

### 修改文件
- `InvestmentDecisionRequest.java` - 添加启用控制字段
- `InvestmentDecisionResponse.java` - 添加工作流信息
- `InvestmentDecisionService.java` - 重构为完整流程
- `InvestmentDecisionController.java` - 更新接口定义
- `InvestmentDecisionControllerTest.java` - 更新测试用例

### 删除文件
- 无

---

## 🧪 测试计划

### 测试用例

| 测试用例 | 描述 | 预期结果 |
|---------|------|---------|
| 完整流程 | 启用所有模块 | 6个步骤全部完成 |
| 禁用RAG | enableRAG=false | 5个步骤，跳过RAG |
| 禁用Tools | enableTools=false | 5个步骤，跳过Tools |
| 禁用Graph | enableGraph=false | 5个步骤，跳过Graph |
| 多轮对话 | 使用相同threadId | 上下文保持 |
| 流式输出 | 使用stream端点 | 逐步返回步骤 |
| 错误处理 | 模块调用失败 | 返回部分结果 |

---

## 📚 文档更新清单

### 需要更新的文档
1. `INVESTMENT_API_GUIDE.md` - 接口使用指南
2. `MANUAL_TEST_GUIDE.md` - 手动测试指南
3. `specs/001-agent-decision-engine/spec.md` - 功能规格
4. `specs/001-agent-decision-engine/plan.md` - 实施计划
5. `specs/001-agent-decision-engine/contracts/api.md` - API 契约

### 更新内容
- 接口请求/响应格式
- 决策流程说明
- 测试用例
- 使用示例

---

## 🚀 如何在新对话中继续开发

### 步骤 1: 提供上下文

在新对话中，提供以下信息：

```
我正在开发一个基于 Spring AI + Langfuse 3 的智能投资代理决策引擎。

项目位置: D:\Program Files\Dev\Workspace\AIProject\springai-langfuse3-demo

当前任务: 重构投资决策主流程接口，串联 Skills + RAG + Tools + Graph

重构计划: specs/001-agent-decision-engine/investment-decision-refactor.md

需要实现:
1. 更新 DTO 类（InvestmentDecisionRequest/Response）
2. 重构 InvestmentDecisionService
3. 更新 InvestmentDecisionController
4. 更新测试用例
5. 更新相关文档

请先阅读重构计划，然后开始实现。
```

### 步骤 2: 关键文件

```
核心文件:
- src/main/java/com/zhou/ai/investment/model/InvestmentDecisionRequest.java
- src/main/java/com/zhou/ai/investment/model/InvestmentDecisionResponse.java
- src/main/java/com/zhou/ai/investment/service/InvestmentDecisionService.java
- src/main/java/com/zhou/ai/investment/controller/InvestmentDecisionController.java
- src/test/java/com/zhou/ai/investment/InvestmentDecisionControllerTest.java

相关模块:
- src/main/java/com/zhou/ai/skills/service/SkillsAgentService.java
- src/main/java/com/zhou/ai/rag/service/RagService.java
- src/main/java/com/zhou/ai/tools/service/StockPriceToolService.java
- src/main/java/com/zhou/ai/tools/service/MarketIndexToolService.java
- src/main/java/com/zhou/ai/tools/service/RiskCalculatorToolService.java
- src/main/java/com/zhou/ai/graph/service/GraphWorkflowService.java
```

### 步骤 3: 验证命令

```bash
# 编译检查
mvn compile

# 运行测试
mvn test -Dtest=InvestmentDecisionControllerTest

# 运行所有测试
mvn test
```

---

## 📅 实施时间表

### Phase 1: DTO 更新（1小时）
- [ ] 更新 InvestmentDecisionRequest
- [ ] 创建 WorkflowResult
- [ ] 创建 WorkflowStep
- [ ] 创建 ToolCall
- [ ] 创建 WorkflowData
- [ ] 更新 InvestmentDecisionResponse

### Phase 2: 服务重构（2小时）
- [ ] 重构 InvestmentDecisionService
- [ ] 实现 RAG 调用
- [ ] 实现 Tools 调用
- [ ] 实现 Graph 调用
- [ ] 实现完整流程编排

### Phase 3: 控制器更新（0.5小时）
- [ ] 更新 InvestmentDecisionController
- [ ] 支持新请求格式

### Phase 4: 测试更新（1小时）
- [ ] 更新测试用例
- [ ] 添加完整流程测试
- [ ] 添加禁用模块测试

### Phase 5: 文档更新（0.5小时）
- [ ] 更新 INVESTMENT_API_GUIDE.md
- [ ] 更新 MANUAL_TEST_GUIDE.md
- [ ] 更新 API 契约

**总计**: 约 5 小时

---

## ✅ 完成标准

### 功能完整性
- [ ] Skills 模块正常工作
- [ ] RAG 模块正常工作
- [ ] Tools 模块正常工作
- [ ] Graph 模块正常工作
- [ ] 完整流程串联成功

### 测试覆盖率
- [ ] 完整流程测试通过
- [ ] 禁用模块测试通过
- [ ] 多轮对话测试通过
- [ ] 流式输出测试通过

### 文档完整性
- [ ] 接口文档更新
- [ ] 使用指南更新
- [ ] 测试文档更新

---

## 📌 注意事项

### 1. 编码问题
Windows 终端默认 GBK，curl 发送中文时使用英文：
```bash
curl -X POST http://localhost:8182/api/investment/decide \
  -H "Content-Type: application/json" \
  -d '{"message": "I want to invest in tech stocks"}'
```

### 2. 依赖检查
确保 pom.xml 包含 reactor-test 依赖：
```xml
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-test</artifactId>
    <scope>test</scope>
</dependency>
```

### 3. 测试超时
投资决策流程较长，测试可能需要 5-10 分钟。

---

## 📚 相关文档

- `INVESTMENT_API_GUIDE.md` - 接口使用指南
- `MANUAL_TEST_GUIDE.md` - 手动测试指南
- `specs/001-agent-decision-engine/spec.md` - 功能规格
- `specs/001-agent-decision-engine/plan.md` - 实施计划

---

**最后更新**: 2026-06-18
**状态**: 待实施
