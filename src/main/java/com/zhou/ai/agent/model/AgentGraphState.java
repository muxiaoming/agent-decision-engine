package com.zhou.ai.agent.model;

/**
 * 多Agent Graph工作流状态键常量定义。
 *
 * <p><b>设计说明：</b>
 * 本常量类集中管理 multiAgent 模式 StateGraph 中所有状态键（State Key），
 * 各Agent节点通过 {@code state.value(KEY)} 读取前序节点结果，
 * 通过 {@code CompletableFuture.completedFuture(Map.of(KEY, value))} 写入自身结果。
 *
 * <p><b>与原有硬编码流程的区别：</b>
 * <ul>
 *   <li><b>原有流程</b>：决策步骤通过手写Flux链 + StringBuilder上下文传递，数据流隐式耦合在闭包中</li>
 *   <li><b>多Agent Graph</b>：每个节点在State中读写结构化状态键，数据流显式可追踪，适合复杂拓扑</li>
 * </ul>
 *
 * <p><b>适用场景：</b>
 * <ul>
 *   <li><b>原有流程</b>：流程固定6步，节点不可逆，适合确定性流水线</li>
 *   <li><b>多Agent Graph</b>：支持条件路由、循环、并行，适合动态编排和扩展</li>
 * </ul>
 *
 * @since 2026-06-30
 */
public final class AgentGraphState {

    private AgentGraphState() { /* 工具类禁止实例化 */ }

    // ==================== 输入参数 ====================

    /** 用户原始消息。由外部调用者设置，所有Agent只读。 */
    public static final String USER_MESSAGE = "user_message";

    /** 模型名称（如 deepSeekChatModel）。由外部调用者设置。 */
    public static final String MODEL_NAME = "model_name";

    /** 是否启用RAG知识检索。由外部调用者设置。 */
    public static final String ENABLE_RAG = "enable_rag";

    /** 是否启用工具调用。由外部调用者设置。 */
    public static final String ENABLE_TOOLS = "enable_tools";

    /** 会话线程ID。由外部调用者设置。 */
    public static final String THREAD_ID = "thread_id";

    /** 流程启动时间戳（毫秒）。在Service层设置，用于计算总耗时。 */
    public static final String START_TIME = "start_time";

    // ==================== Agent输出 ====================

    /** 意图分类Agent输出：LLM对用户意图的分类描述文本。 */
    public static final String INTENT_RESULT = "intent_result";

    /** 是否为投资相关意图（布尔值字符串 "true"/"false"）。 */
    public static final String IS_INVESTMENT = "is_investment";

    /** 问题感知Agent输出：对用户需求的分析和理解结果。 */
    public static final String PERCEPTION_RESULT = "perception_result";

    /** 知识检索Agent输出：RAG检索到的知识文本摘要。 */
    public static final String RETRIEVAL_RESULT = "retrieval_result";

    /** 知识检索的完成状态："completed" / "skipped"。 */
    public static final String RETRIEVAL_STATUS = "retrieval_status";

    /** 数据获取Agent输出：工具调用获取的市场数据文本摘要。 */
    public static final String DATA_RESULT = "data_result";

    /** 数据获取的完成状态："completed" / "skipped"。 */
    public static final String DATA_STATUS = "data_status";

    /** 推理分析Agent输出：基于知识和数据的深度分析结果。 */
    public static final String REASONING_RESULT = "reasoning_result";

    /** 决策生成Agent输出：最终投资建议文本。 */
    public static final String DECISION_RESULT = "decision_result";

    /** 流程编排Agent输出：整体决策流程的验证和总结。 */
    public static final String SCHEDULE_RESULT = "schedule_result";

    // ==================== 公共上下文 ====================

    /** 累计上下文文本（各Agent按序追加）。 */
    public static final String CONTEXT = "context";

    /** 风险提示模板文本。在Service层预设，final事件携带。 */
    public static final String RISK_WARNING = "risk_warning";
}
