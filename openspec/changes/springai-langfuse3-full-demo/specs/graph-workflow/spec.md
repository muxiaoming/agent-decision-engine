## ADDED Requirements

### Requirement: StateGraph workflow definition

The system SHALL define graph workflows using Spring AI Alibaba's `StateGraph` API, supporting sequential, parallel, and conditional branching node execution.

#### Scenario: Define a sequential workflow
- **WHEN** a `StateGraph` bean is configured with nodes A → B → C connected by sequential edges
- **THEN** the graph executes nodes in order: A, then B, then C

#### Scenario: Define a parallel workflow
- **WHEN** a `StateGraph` bean has a fan-out node that connects to multiple parallel nodes
- **THEN** the parallel nodes execute concurrently and results are merged at the fan-in node

#### Scenario: Define a conditional branching workflow
- **WHEN** a `StateGraph` bean has a conditional edge that routes based on node output
- **THEN** the graph follows the appropriate branch based on the condition evaluation result

### Requirement: Graph workflow REST endpoint

The system SHALL expose `POST /api/graph/workflow` that triggers a predefined graph workflow with input parameters and returns the execution result.

#### Scenario: Execute sequential workflow
- **WHEN** client sends `POST /api/graph/workflow` with `{"workflow": "sequential", "input": "Analyze this text..."}`
- **THEN** the graph executes all nodes in sequence and returns the final output

#### Scenario: Execute conditional workflow
- **WHEN** client sends `POST /api/graph/workflow` with `{"workflow": "conditional", "input": "Is this positive or negative?"}`
- **THEN** the graph evaluates the condition and routes to the appropriate branch, returning the result

### Requirement: Graph execution state management

The system SHALL maintain `OverAllState` throughout graph execution, allowing nodes to read from and write to shared state.

#### Scenario: State shared between nodes
- **WHEN** node A writes `classification = "positive"` to OverAllState
- **THEN** subsequent node B can read `classification` from the same OverAllState

### Requirement: Graph workflow observability

Each graph node execution SHALL be traced as an individual OpenTelemetry span within the parent trace.

#### Scenario: Graph nodes traced in Langfuse
- **WHEN** a graph workflow with 3 nodes executes
- **THEN** Langfuse shows a parent trace with 3 child spans, one per node execution, including input/output and duration
