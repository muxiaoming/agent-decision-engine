# Tasks: Spring AI + Langfuse3 完整演示项目

**Input**: Design documents from `specs/001-springai-langfuse3-demo/`

**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/api.md ✅, quickstart.md ✅, constitution.md ✅

**Tests**: 宪章原则 IV 要求测试先行，因此每个用户故事包含对应测试任务。

**Organization**: 按用户故事组织任务，支持独立实现和测试。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行执行（不同文件，无依赖）
- **[Story]**: 所属用户故事（US1-US6）
- 每个任务包含精确文件路径

---

## Phase 1: Setup（项目初始化）

**Purpose**: 创建 Maven 项目结构、Spring Boot 主类、配置文件骨架

- [x] T001 创建 Maven 项目结构和 pom.xml，配置 Spring Boot 3.4.x 父 POM、Spring AI 1.1.x BOM、Spring AI Alibaba 1.1.x、Micrometer Tracing、OpenTelemetry OTLP 导出器等依赖在 `pom.xml`
- [x] T002 创建 Spring Boot 主类在 `src/main/java/com/example/ai/SpringAiDemoApplication.java`
- [x] T003 创建主配置文件在 `src/main/resources/application.yml`，包含基础应用配置和 Spring AI 占位配置
- [x] T004 [P] 创建 Langfuse 云端 Profile 配置在 `src/main/resources/application-cloud.yml`
- [x] T005 [P] 创建 Langfuse 本地 Profile 配置在 `src/main/resources/application-local.yml`

---

## Phase 2: Foundational（公共基础设施 — 阻塞所有用户故事）

**Purpose**: 构建 common 模块，所有功能模块依赖此层

**⚠️ CRITICAL**: 必须在任何用户故事开始前完成

- [x] T006 [P] 创建 ChatRequest DTO 在 `src/main/java/com/example/ai/common/model/ChatRequest.java`
- [x] T007 [P] 创建 ChatResponse DTO 在 `src/main/java/com/example/ai/common/model/ChatResponse.java`
- [x] T008 [P] 创建 TokenUsage DTO 在 `src/main/java/com/example/ai/common/model/TokenUsage.java`
- [x] T009 创建 AiModelConfig 多模型配置类，从 `application.yml` 读取 DeepSeek/MiMo/DashScope 三模型配置在 `src/main/java/com/example/ai/common/config/AiModelConfig.java`
- [x] T010 创建 ChatClientConfig，为每个模型注册 ChatClient Bean 在 `src/main/java/com/example/ai/common/config/ChatClientConfig.java`
- [x] T011 创建 ModelRouter 模型路由器，根据请求中的 model 参数路由到对应 ChatClient 在 `src/main/java/com/example/ai/common/router/ModelRouter.java`
- [x] T012 [P] 创建 ModelNotAvailableException 异常类在 `src/main/java/com/example/ai/common/exception/ModelNotAvailableException.java`
- [x] T013 创建 GlobalExceptionHandler 全局异常处理器，处理模型不可用等业务异常在 `src/main/java/com/example/ai/common/exception/GlobalExceptionHandler.java`

**Checkpoint**: 公共基础设施就绪 — 用户故事实现可开始并行推进

---

## Phase 3: User Story 1 — 多模型自由切换对话（Priority: P1）🎯 MVP

**Goal**: 实现 DeepSeek/MiMo/DashScope 三模型同步对话，通过请求参数自由切换

**Independent Test**: 向三个模型分别发送同一问题，验证每个模型均能正常返回，切换模型无需重启

### Implementation for User Story 1

- [x] T014 [US1] 实现 ChatService 对话服务层，注入 ModelRouter 执行模型调用，在 `src/main/java/com/example/ai/chat/service/ChatService.java`
- [x] T015 [US1] 实现 ChatController 同步对话端点 `POST /api/chat`，对应契约 api.md §1 在 `src/main/java/com/example/ai/chat/controller/ChatController.java`
- [x] T016 [US1] 编写 ChatController 单元测试，验证正常对话、模型切换和模型不可用错误处理在 `src/test/java/com/example/ai/chat/ChatControllerTest.java`

**Checkpoint**: User Story 1 完成 — 用户可通过 POST /api/chat 使用任意已配置模型对话

---

## Phase 4: User Story 2 — 三种流式实时对话（Priority: P1）

**Goal**: 实现 Flux/SSE/SseEmitter 三种独立流式输出端点

**Independent Test**: 分别调用三个流式接口，验证逐步返回 AI 回答（打字机效果）

### Implementation for User Story 2

- [x] T017 [P] [US2] 实现 FluxStreamController — Flux 响应式流式端点 `GET /api/chat/flux/stream`，对应契约 api.md §2 在 `src/main/java/com/example/ai/chat/controller/FluxStreamController.java`
- [x] T018 [P] [US2] 实现 SseStreamController — SSE 标准流式端点 `GET /api/chat/sse/stream`，对应契约 api.md §3 在 `src/main/java/com/example/ai/chat/controller/SseStreamController.java`
- [x] T019 [P] [US2] 实现 SseEmitterController — SseEmitter 流式端点 `GET /api/chat/emitter/stream`，对应契约 api.md §4 在 `src/main/java/com/example/ai/chat/controller/SseEmitterController.java`
- [x] T020 [US2] 编写流式端点集成测试，验证三种流式输出均能正确返回事件流在 `src/test/java/com/example/ai/chat/FluxStreamControllerTest.java`

**Checkpoint**: User Stories 1 和 2 完成 — 同步对话 + 三种流式输出均可独立测试

---

## Phase 5: User Story 3 — RAG 本地知识库问答（Priority: P2）

**Goal**: 实现文档摄入、向量嵌入、相关性检索和 RAG 问答的完整流程

**Independent Test**: 导入测试文档后针对文档内容提问，验证基于文档返回准确回答

### Implementation for User Story 3

- [x] T021 [US3] 创建 VectorStoreConfig 向量存储配置，注册 SimpleVectorStore Bean 和 EmbeddingModel 在 `src/main/java/com/example/ai/rag/config/VectorStoreConfig.java`
- [x] T022 [US3] 实现 DocumentIngestionService 文档摄入服务，完成文件读取→TokenTextSplitter 分割→VectorStore.add() 流程在 `src/main/java/com/example/ai/rag/service/DocumentIngestionService.java`
- [x] T023 [US3] 实现 RagService RAG 问答服务，使用 QuestionAnswerAdvisor 完成检索增强生成，支持同步和流式在 `src/main/java/com/example/ai/rag/service/RagService.java`
- [x] T024 [US3] 实现 RagController RAG 问答端点 `POST /api/rag/ask`、`GET /api/rag/ask/stream`、`POST /api/rag/ingest`，对应契约 api.md §5 在 `src/main/java/com/example/ai/rag/controller/RagController.java`
- [x] T025 [P] [US3] 创建 RAG 示例文档在 `src/main/resources/docs/sample.txt`
- [x] T026 [US3] 编写 RagService 单元测试，验证文档摄入和 RAG 问答流程在 `src/test/java/com/example/ai/rag/RagServiceTest.java`

**Checkpoint**: User Story 3 完成 — RAG 知识库问答可独立运行

---

## Phase 6: User Story 4 — Function Calling 工具调用（Priority: P2)

**Goal**: 实现 AI 模型根据用户意图自动调用外部工具（天气查询、数学计算）

**Independent Test**: 发送"北京今天天气如何"，验证自动调用天气工具并整合结果回答

### Implementation for User Story 4

- [x] T027 [P] [US4] 实现 WeatherFunction 天气查询工具（模拟数据），注册为 Spring Bean 在 `src/main/java/com/example/ai/tools/functions/WeatherFunction.java`
- [x] T028 [P] [US4] 实现 CalculatorFunction 数学计算工具，注册为 Spring Bean 在 `src/main/java/com/example/ai/tools/functions/CalculatorFunction.java`
- [x] T029 [US4] 实现 ToolChatService 工具调用服务，通过 ChatClient.toolNames() 指定可用工具执行 Function Calling 在 `src/main/java/com/example/ai/tools/service/ToolChatService.java`
- [x] T030 [US4] 实现 ToolChatController Function Calling 端点 `POST /api/tools/chat`，对应契约 api.md §6 在 `src/main/java/com/example/ai/tools/controller/ToolChatController.java`
- [x] T031 [US4] 编写 ToolChatService 单元测试，验证工具调用、普通闲聊不触发工具两种场景在 `src/test/java/com/example/ai/tools/ToolChatServiceTest.java`

**Checkpoint**: User Story 4 完成 — Function Calling 可独立测试

---

## Phase 7: User Story 5 — Graph 多步骤工作流（Priority: P3)

**Goal**: 实现基于 StateGraph 的多步骤 AI 工作流，包含分类→处理→输出节点和条件分支

**Independent Test**: 提交文本后验证各节点按顺序执行，条件分支按分类结果正确路由

### Implementation for User Story 5

- [x] T032 [US5] 创建 WorkflowGraphConfig Graph 定义配置，使用 StateGraph 定义分类节点、处理节点、输出节点和条件路由边在 `src/main/java/com/example/ai/graph/config/WorkflowGraphConfig.java`
- [x] T033 [US5] 实现 GraphWorkflowService Graph 工作流服务，编译并执行图工作流在 `src/main/java/com/example/ai/graph/service/GraphWorkflowService.java`
- [x] T034 [US5] 实现 GraphController 工作流端点 `POST /api/graph/execute`，对应契约 api.md §7 在 `src/main/java/com/example/ai/graph/controller/GraphController.java`
- [x] T035 [US5] 编写 GraphWorkflowService 单元测试，验证工作流节点顺序执行和条件分支路由在 `src/test/java/com/example/ai/graph/GraphWorkflowServiceTest.java`

**Checkpoint**: User Story 5 完成 — Graph 工作流可独立测试

---

## Phase 8: User Story 6 — Langfuse 全链路可观测（Priority: P3)

**Goal**: 通过 OpenTelemetry OTLP 将全链路追踪上报至 Langfuse，支持云端和本地 Docker 双模式

**Independent Test**: 发送 AI 请求后 30 秒内在 Langfuse 界面查看完整 Trace

### Implementation for User Story 6

- [x] T036 [P] [US6] 创建 OtelConfig OpenTelemetry 配置，配置 OTLP 导出器在 `src/main/java/com/example/ai/observability/config/OtelConfig.java`
- [x] T037 [P] [US6] 创建 LangfuseConfig Langfuse 双模式配置，通过 langfuse.mode 属性切换云端/本地模式，配置 OTLP Endpoint 和认证 Headers 在 `src/main/java/com/example/ai/observability/config/LangfuseConfig.java`
- [x] T038 [US6] 实现 ObservabilityController 可观测性健康检查端点 `GET /api/observability/health`，对应契约 api.md §8 在 `src/main/java/com/example/ai/observability/controller/ObservabilityController.java`
- [x] T039 [US6] 完善 application-cloud.yml 和 application-local.yml 中的 Langfuse OTLP 配置，包含 Endpoint、Headers（Base64 编码的 publicKey:secretKey）在 `src/main/resources/application-cloud.yml` 和 `src/main/resources/application-local.yml`
- [x] T040 [US6] 编写 Langfuse 集成测试，验证 OTel Span 正确导出和 Trace 数据完整性在 `src/test/java/com/example/ai/observability/LangfuseIntegrationTest.java`

**Checkpoint**: 所有用户故事完成

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: 部署文件、示例文档、跨模块整合

- [x] T041 [P] 创建 Langfuse 本地 Docker Compose 部署文件在 `docker/docker-compose-langfuse.yml`
- [x] T042 编写 ModelRouter 单元测试，验证模型路由逻辑和不可用模型处理在 `src/test/java/com/example/ai/common/ModelRouterTest.java`
- [x] T043 运行 quickstart.md 验证全流程：启动应用 → 多模型对话 → 流式输出 → RAG → Function Calling → Graph → Langfuse Trace

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: 无依赖 — 立即开始
- **Phase 2 (Foundational)**: 依赖 Phase 1 — 阻塞所有用户故事
- **Phase 3-8 (User Stories)**: 全部依赖 Phase 2 完成
  - US1 和 US2 共享 ChatService，US2 依赖 US1 的 ChatService
  - US3 (RAG)、US4 (Function Calling)、US5 (Graph) 互不依赖，可并行
  - US6 (Observability) 为横切关注点，可与其他故事并行
- **Phase 9 (Polish)**: 依赖所有用户故事完成

### User Story Dependencies

- **US1 (P1 多模型对话)**: Phase 2 完成后可开始 — 无其他故事依赖
- **US2 (P1 流式输出)**: Phase 2 完成后可开始 — 复用 ChatService（来自 US1），但 Flux/SSE/SseEmitter 控制器可与 US1 并行开发
- **US3 (P2 RAG)**: Phase 2 完成后可开始 — 独立于 US1/US2
- **US4 (P2 Function Calling)**: Phase 2 完成后可开始 — 独立于 US1/US2
- **US5 (P3 Graph)**: Phase 2 完成后可开始 — 独立于其他故事
- **US6 (P3 Observability)**: Phase 2 完成后可开始 — 横切关注点，可与其他故事并行

### Within Each User Story

- Models/DTOs → Services → Controllers → Tests
- 每个故事内的 [P] 标记任务可并行执行

### Parallel Opportunities

```text
Phase 2 完成后:

并行组 A（独立模块，互不依赖）:
  ├── US3: RAG (T021-T026)
  ├── US4: Function Calling (T027-T031)
  ├── US5: Graph (T032-T035)
  └── US6: Observability (T036-T040)

顺序组（共享 ChatService）:
  US1 (T014-T016) → US2 (T017-T020)
```

---

## Parallel Example: User Story 3 (RAG)

```bash
# T021 和 T025 可并行（不同文件，无依赖）:
Task: "创建 VectorStoreConfig 在 src/main/java/.../rag/config/VectorStoreConfig.java"
Task: "创建 RAG 示例文档在 src/main/resources/docs/sample.txt"

# T022 和 T023 顺序执行（T023 依赖 T022）:
Task: "实现 DocumentIngestionService" → "实现 RagService" → "实现 RagController"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. 完成 Phase 1: Setup
2. 完成 Phase 2: Foundational（CRITICAL — 阻塞所有故事）
3. 完成 Phase 3: User Story 1（多模型对话）
4. **STOP and VALIDATE**: 用 curl 测试 POST /api/chat，验证三个模型切换
5. 可部署/演示

### Incremental Delivery

1. Setup + Foundational → 基础就绪
2. + US1 → 同步多模型对话（MVP!）
3. + US2 → 三种流式输出
4. + US3 → RAG 知识库问答
5. + US4 → Function Calling
6. + US5 → Graph 工作流
7. + US6 → Langfuse 全链路可观测
8. Polish → 部署文件、验证、清理

### Parallel Team Strategy

多个开发者并行开发:

1. 团队共同完成 Setup + Foundational
2. Phase 2 完成后:
   - 开发者 A: US1 → US2（顺序，共享 ChatService）
   - 开发者 B: US3 (RAG)
   - 开发者 C: US4 (Function Calling)
   - 开发者 D: US5 (Graph) + US6 (Observability)

---

## Notes

- [P] tasks = 不同文件，无依赖，可并行
- [Story] 标签将任务映射到具体用户故事
- 每个用户故事可独立完成和测试
- 提交前必须通过 `mvn test`
- 每个检查点后验证故事独立功能
- API 密钥通过环境变量注入，不得硬编码
- 所有文档、注释、提交信息使用简体中文
