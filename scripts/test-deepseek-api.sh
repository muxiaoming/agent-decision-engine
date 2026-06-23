#!/bin/bash

# DeepSeek API 测试脚本

echo "========================================="
echo "🔍 检查 DeepSeek API 配置"
echo "========================================="
echo ""

# 1. 检查环境变量
echo "1️⃣ 检查环境变量:"
if [ -z "$DEEPSEEK_API_KEY" ]; then
    echo "   ❌ DEEPSEEK_API_KEY 未设置"
    echo ""
    echo "   设置方法:"
    echo "   export DEEPSEEK_API_KEY=sk-your-real-api-key"
else
    echo "   ✅ DEEPSEEK_API_KEY 已设置 (长度: ${#DEEPSEEK_API_KEY})"
fi
echo ""

# 2. 检查配置文件
echo "2️⃣ 检查 application.yml 配置:"
if grep -q "your-deepseek-api-key" src/main/resources/application.yml 2>/dev/null; then
    echo "   ⚠️  使用默认配置 (your-deepseek-api-key)"
elif [ -n "$DEEPSEEK_API_KEY" ]; then
    echo "   ✅ API Key 从环境变量读取"
else
    echo "   ❌ 未找到有效配置"
fi
echo ""

# 3. 测试 API 连接
echo "3️⃣ 测试 DeepSeek API 连接:"
if [ -n "$DEEPSEEK_API_KEY" ]; then
    echo "   发送测试请求..."

    RESPONSE=$(curl -s -w "\n%{http_code}" \
        -X POST https://api.deepseek.com/chat/completions \
        -H "Authorization: Bearer $DEEPSEEK_API_KEY" \
        -H "Content-Type: application/json" \
        -d '{
            "model": "deepseek-chat",
            "messages": [{"role": "user", "content": "Hello"}],
            "max_tokens": 10
        }' 2>&1)

    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" = "200" ]; then
        echo "   ✅ API 连接成功 (HTTP $HTTP_CODE)"
        echo "   响应: $(echo $BODY | head -c 100)..."
    elif [ "$HTTP_CODE" = "401" ]; then
        echo "   ❌ API Key 无效 (HTTP $HTTP_CODE)"
        echo "   请检查 API Key 是否正确"
    elif [ "$HTTP_CODE" = "429" ]; then
        echo "   ⚠️  请求频率过高 (HTTP $HTTP_CODE)"
        echo "   请等待后重试"
    else
        echo "   ❌ API 连接失败 (HTTP $HTTP_CODE)"
        echo "   响应: $BODY"
    fi
else
    echo "   ⏭️  跳过 (API Key 未设置)"
fi
echo ""

# 4. 总结
echo "========================================="
echo "📊 诊断总结"
echo "========================================="
echo ""

if [ -z "$DEEPSEEK_API_KEY" ]; then
    echo "❌ 主要问题: DeepSeek API Key 未设置"
    echo ""
    echo "影响范围:"
    echo "   ❌ Skills 步骤 (问题感知、推理分析、决策生成) - 需要 AI"
    echo "   ❌ RAG 步骤 (知识检索) - 需要 AI"
    echo "   ✅ Tools 步骤 (数据获取) - 不需要 AI"
    echo "   ✅ Graph 步骤 (流程编排) - 不需要 AI"
    echo ""
    echo "解决方案:"
    echo "   1. 设置 API Key: export DEEPSEEK_API_KEY=sk-your-key"
    echo "   2. 重启应用: mvn spring-boot:run"
    echo "   3. 重新测试"
else
    echo "✅ DeepSeek API Key 已配置"
    echo ""
    echo "如果仍有问题，请检查:"
    echo "   1. API Key 是否有效"
    echo "   2. 网络连接是否正常"
    echo "   3. 查看应用日志: tail -f logs/application.log"
fi
echo ""
