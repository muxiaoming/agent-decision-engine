# 快速启动指南：Spring AI + Langfuse3 演示项目

**功能**: 001-springai-langfuse3-demo
**日期**: 2026-06-04

## 前置条件

- Java 21 或更高版本
- Maven 3.9+
- Docker 和 Docker Compose（仅本地 Langfuse 模式需要）
- 至少一个 AI 模型的 API 密钥

## 1. 克隆项目

```bash
git clone <仓库地址>
cd springai-langfuse3-demo
```

## 2. 配置 API 密钥

复制配置模板并填入你的 API 密钥：

```bash
cp src/main/resources/application-template.yml \
   src/main/resources/application-local.yml
```

编辑 `application-local.yml`，填入至少一个模型的 API 密钥：

```yaml
spring:
  ai:
    deepseek:
      api-key: ${DEEPSEEK_API_KEY:}
    openai:
      api-key: ${MIMO_API_KEY:}
      base-url: https://api.mimo.example.com
      chat:
        model: mimo-chat
    alibaba:
      dashscope:
        api-key: ${DASHSCOPE_API_KEY:}
```

## 3. 选择 Langfuse 模式

### 云端模式（推荐新手）

1. 注册 [Langfuse Cloud](https://langfuse.com)
2. 创建项目，获取 Public Key 和 Secret Key
3. 在 `application-local.yml` 中配置：

```yaml
langfuse:
  mode: cloud
  cloud:
    endpoint: https://cloud.langfuse.com/api/public/otel
    public-key: ${LANGFUSE_PUBLIC_KEY}
    secret-key: ${LANGFUSE_SECRET_KEY}
```

### 本地 Docker 模式

```bash
# 启动 Langfuse 本地实例
docker-compose -f docker/docker-compose-langfuse.yml up -d

# 在 application-local.yml 中配置
langfuse:
  mode: local
  local:
    endpoint: http://localhost:3000/api/public/otel
```

等待 Langfuse 启动完成（约 30 秒），访问 http://localhost:3000
创建项目并获取密钥。

## 4. 启动应用

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## 5. 验证功能

### 多模型对话

```bash
# DeepSeek 对话
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"你好","model":"deepseek"}'
```

### 流式对话

```bash
# Flux 流式
curl -N "http://localhost:8080/api/chat/flux/stream?message=你好&model=deepseek"

# SSE 流式
curl -N "http://localhost:8080/api/chat/sse/stream?message=你好&model=deepseek"

# SseEmitter 流式
curl -N "http://localhost:8080/api/chat/emitter/stream?message=你好&model=deepseek"
```

### RAG 问答

```bash
# 摄入文档
curl -X POST http://localhost:8080/api/rag/ingest \
  -F "file=@docs/sample.txt"

# 知识库问答
curl -X POST http://localhost:8080/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"message":"文档中提到了什么","model":"deepseek"}'
```

### Function Calling

```bash
curl -X POST http://localhost:8080/api/tools/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"北京今天天气如何","model":"deepseek"}'
```

### Graph 工作流

```bash
curl -X POST http://localhost:8080/api/graph/execute \
  -H "Content-Type: application/json" \
  -d '{"input":"今天天气真好","model":"dashscope"}'
```

### Skills Agent（新增）

```bash
# 列出可用技能
curl http://localhost:8080/api/skills

# Skills Agent 对话 - 市场分析
curl -X POST http://localhost:8080/api/skills/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "分析一下A股市场"}'

# Skills Agent 对话 - 天气查询
curl -X POST http://localhost:8080/api/skills/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "北京今天天气怎么样？"}'

# Skills Agent 多轮对话
curl -X POST http://localhost:8080/api/skills/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "北京今天天气怎么样？", "threadId": "test-123"}'

curl -X POST http://localhost:8080/api/skills/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "那穿什么衣服合适？", "threadId": "test-123"}'
```

### 投资工具（新增）

```bash
# 股价查询
curl -X POST http://localhost:8080/api/tools/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "查询苹果公司的股价"}'

# 市场指数查询
curl -X POST http://localhost:8080/api/tools/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "查询上证指数"}'

# 风险计算
curl -X POST http://localhost:8080/api/tools/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "帮我计算60%股票、30%债券、10%现金的组合收益"}'

# VaR 计算
curl -X POST http://localhost:8080/api/tools/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "我投资了10万元，帮我计算95%置信水平下30天的VaR"}'
```

### 查看 Trace

发送请求后，打开 Langfuse 界面（云端或本地）查看 Trace 记录，
确认包含完整的调用链路信息。

## 6. 运行测试

```bash
mvn test
```

## 项目结构

```text
springai-langfuse3-demo/
├── pom.xml                          # Maven 父 POM
├── docker/
│   └── docker-compose-langfuse.yml  # Langfuse 本地部署
├── src/main/java/com/zhou/ai/
│   ├── common/                      # 公共配置和模型路由
│   ├── chat/                        # 流式对话端点（含系统提示词）
│   ├── rag/                         # RAG 知识库问答（含系统提示词）
│   ├── tools/                       # Function Calling（含投资工具）
│   │   └── service/
│   │       ├── StockPriceToolService.java   # 股价查询工具（新增）
│   │       ├── MarketIndexToolService.java  # 市场指标工具（新增）
│   │       └── RiskCalculatorToolService.java # 风险计算工具（新增）
│   ├── skills/                      # Skills 框架（新增）
│   │   ├── controller/
│   │   │   └── SkillsAgentController.java
│   │   ├── service/
│   │   │   └── SkillsAgentService.java
│   │   └── config/
│   │       └── SkillsAgentConfig.java
│   ├── graph/                       # Graph 工作流
│   └── observability/               # OTel + Langfuse 配置
├── src/main/resources/
│   ├── application.yml              # 主配置
│   ├── application-cloud.yml        # Langfuse 云端配置
│   ├── application-local.yml        # Langfuse 本地配置
│   ├── docs/                        # RAG 示例文档
│   └── skills/                      # 技能定义目录（新增）
│       ├── market-analysis/
│       │   └── SKILL.md
│       ├── risk-assessment/
│       │   └── SKILL.md
│       ├── portfolio-optimization/
│       │   └── SKILL.md
│       ├── investment-recommendation/
│       │   └── SKILL.md
│       ├── weather-assistant/
│       │   └── SKILL.md
│       ├── java-spring-expert/
│       │   └── SKILL.md
│       └── code-reviewer/
│           └── SKILL.md
└── src/test/                        # 测试代码
    └── java/com/zhou/ai/
        ├── ApplicationIntegrationTest.java  # 主流程冒烟测试（新增）
        └── skills/
            ├── SkillsAgentIntegrationTest.java
            └── SkillsAgentServiceTest.java
```
