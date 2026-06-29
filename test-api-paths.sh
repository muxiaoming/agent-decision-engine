#!/bin/bash
# API 路径配置验证脚本

BASE_URL="http://localhost:8182"
API_PREFIX="/api"

echo "=========================================="
echo "   API 路径配置验证"
echo "=========================================="
echo ""

# 测试函数
test_endpoint() {
    local endpoint=$1
    local description=$2
    local url="${BASE_URL}${API_PREFIX}${endpoint}"

    echo -n "测试 ${description}: ${url} ... "

    response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null)

    if [ "$response" = "200" ] || [ "$response" = "405" ]; then
        echo "✅ 成功 (HTTP $response)"
        return 0
    elif [ "$response" = "000" ]; then
        echo "❌ 连接失败 (服务未启动)"
        return 1
    else
        echo "⚠️  HTTP $response"
        return 0
    fi
}

echo "正在验证 API 端点..."
echo ""

# Chat 模块
test_endpoint "/chat/models" "Chat 模块 - 查询可用模型"
test_endpoint "/chat/flux/stream?message=hi" "Chat 模块 - Flux 流式"
test_endpoint "/chat/emitter/stream?message=hi" "Chat 模块 - SseEmitter 流式"
test_endpoint "/chat/sse/stream?message=hi" "Chat 模块 - SSE 流式"

# Investment 模块
test_endpoint "/investment/health" "投资决策模块 - 健康检查"
test_endpoint "/skills" "技能模块 - 列出所有技能"

# Graph 模块
test_endpoint "/graph" "Graph 模块 - 工作流"

# RAG 模块
test_endpoint "/rag" "RAG 模块 - 知识库"

# Tools 模块
test_endpoint "/tools" "Tools 模块 - 工具"

# Observability 模块
test_endpoint "/observability/health" "可观测性模块 - Langfuse 健康检查"

echo ""
echo "=========================================="
echo "   验证完成"
echo "=========================================="
echo ""
echo "如果所有测试通过，说明 API 路径配置正确！"
echo ""
echo "Swagger 文档: ${BASE_URL}${API_PREFIX}/swagger-ui.html"
echo ""
