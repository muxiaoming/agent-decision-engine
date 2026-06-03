## ADDED Requirements

### Requirement: OTLP Exporter configuration for Langfuse

The system SHALL configure OpenTelemetry OTLP HTTP Exporter to send traces to the Langfuse OTLP endpoint via environment variables or `application.yml`.

#### Scenario: Configure via environment variables
- **WHEN** `OTEL_EXPORTER_OTLP_ENDPOINT` and `OTEL_EXPORTER_OTLP_HEADERS` environment variables are set
- **THEN** all OpenTelemetry traces are exported to the specified Langfuse endpoint

#### Scenario: Configure via application.yml
- **WHEN** `application.yml` contains `app.langfuse.endpoint`, `app.langfuse.public-key`, and `app.langfuse.secret-key`
- **THEN** the application constructs the OTLP headers (Basic Auth) and endpoint at startup

### Requirement: Automatic Spring AI trace instrumentation

The system SHALL automatically capture ChatModel calls, embeddings, and retriever operations as OpenTelemetry spans without manual instrumentation code.

#### Scenario: Chat call traced automatically
- **WHEN** a `/api/chat` request triggers a DeepSeek API call
- **THEN** a trace is created with spans for: HTTP request → ChatModel call → LLM generation → response

#### Scenario: RAG pipeline traced automatically
- **WHEN** a `/api/rag/query` request triggers retrieval and generation
- **THEN** a trace is created with spans for: embedding → vector search → prompt construction → LLM generation

### Requirement: Langfuse trace enrichment with metadata

The system SHALL attach metadata (model name, user ID, session ID) to traces so they are searchable and filterable in Langfuse.

#### Scenario: Model name in trace metadata
- **WHEN** a chat request uses the DashScope model
- **THEN** the Langfuse trace contains `gen_ai.request.model: qwen-plus` in its attributes

#### Scenario: Session grouping
- **WHEN** multiple requests share the same `session_id` header
- **THEN** Langfuse groups these traces under a single session

### Requirement: Local and cloud Langfuse support

The system SHALL support both Langfuse Cloud and self-hosted Langfuse instances by configuring the OTLP endpoint URL.

#### Scenario: Connect to Langfuse Cloud
- **WHEN** endpoint is set to `https://cloud.langfuse.com/api/public/otel`
- **THEN** traces are exported to Langfuse Cloud EU region

#### Scenario: Connect to self-hosted Langfuse
- **WHEN** endpoint is set to `http://localhost:3000/api/public/otel`
- **THEN** traces are exported to the local Langfuse instance
