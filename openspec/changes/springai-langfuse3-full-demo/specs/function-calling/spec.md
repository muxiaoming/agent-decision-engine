## ADDED Requirements

### Requirement: Tool function registration

The system SHALL register custom tool functions that can be invoked by the LLM during chat completions.

#### Scenario: Register weather tool
- **WHEN** a `@Bean` method annotated with `@Description` returns a `Function<WeatherRequest, WeatherResponse>` 
- **THEN** the tool is registered and available for LLM function calling

#### Scenario: Register multiple tools
- **WHEN** multiple tool functions are defined (e.g., weather, calculator, stock price)
- **THEN** all tools are registered and the LLM can select the appropriate tool based on user intent

### Requirement: Function calling REST endpoint

The system SHALL expose `POST /api/function-call` that sends a user message to the LLM with registered tools enabled, allowing the model to call tools and return the final answer.

#### Scenario: LLM invokes tool
- **WHEN** client sends `POST /api/function-call` with `{"message": "What's the weather in Beijing?", "model": "deepseek"}`
- **THEN** the LLM calls the weather tool with `city=Beijing`, receives the result, and returns a natural language answer incorporating the tool result

#### Scenario: No tool invocation needed
- **WHEN** client sends `POST /api/function-call` with `{"message": "Tell me a joke", "model": "deepseek"}`
- **THEN** the LLM responds directly without invoking any tool

### Requirement: Tool call tracing

The system SHALL log each tool invocation (function name, input arguments, output result) as part of the OpenTelemetry trace span.

#### Scenario: Tool call visible in Langfuse
- **WHEN** a function call is executed during a chat request
- **THEN** the Langfuse trace shows the tool call as a child span with function name, input, and output
