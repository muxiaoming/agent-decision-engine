#!/bin/bash

# DeepSeek API 配置验证脚本

echo "========================================="
echo "🔍 验证 DeepSeek API 配置"
echo "========================================="
echo ""

PROJECT_DIR="D:\Program Files\Dev\Workspace\AIProject\springai-langfuse3-demo"
cd "$PROJECT_DIR" 2>/dev/null || {
    echo "❌ 无法进入项目目录"
    exit 1
}

echo "📁 项目目录: $PROJECT_DIR"
echo ""

# 1. 检查配置文件
echo "1️⃣ 检查配置文件:"
echo ""

echo "   application.yml 中的 active profiles:"
grep -A 1 "active:" src/main/resources/application.yml | head -1
echo ""

echo "   application-dev.yml 中的 API Keys:"
if [ -f src/main/resources/application-dev.yml ]; then
    echo "   ✅ 找到 application-dev.yml"
    grep "api-key:" src/main/resources/application-dev.yml | head -3
else
    echo "   ❌ application-dev.yml 不存在"
fi
echo ""

# 2. 检查环境变量（可能影响配置）
echo "2️⃣ 检查环境变量（可能覆盖配置）:"
echo "   DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY:-(未设置)}"
echo "   MIMO_API_KEY=${MIMO_API_KEY:-(未设置)}"
echo "   DASHSCOPE_API_KEY=${DASHSCOPE_API_KEY:-(未设置)}"
echo ""

# 3. 验证 API Key 格式
echo "3️⃣ 验证 API Key 格式:"
if grep -q "sk-ec1404d8803544dbb0a3db8b9acb346d" src/main/resources/application-dev.yml 2>/dev/null; then
    echo "   ✅ DeepSeek API Key 格式正确 (sk-ec1404d8...)"
    API_KEY=$(grep "deepseek:" -A 1 src/main/resources/application-dev.yml | grep "api-key:" | cut -d' ' -f4)
    echo "   Key 长度: ${#API_KEY} 字符"
else
    echo "   ❌ 未找到有效的 API Key"
fi
echo ""

# 4. 检查是否需要环境变量
echo "4️⃣ 检查配置优先级:"
echo ""
echo "   优先级顺序 (从高到低):"
echo "   1. 环境变量 \$DEEPSEEK_API_KEY"
echo "   2. application-dev.yml (当前激活)"
echo "   3. application.yml (默认值)"
echo ""

if [ -n "$DEEPSEEK_API_KEY" ]; then
    echo "   ⚠️  环境变量已设置，将覆盖 application-dev.yml"
else
    echo "   ✅ 环境变量未设置，将使用 application-dev.yml 的值"
fi
echo ""

# 5. 测试 API 连接
echo "5️⃣ 测试 DeepSeek API 连接:"
echo ""

# 从配置文件读取 API Key
CONFIG_API_KEY=$(grep "api-key: sk-" src/main/resources/application-dev.yml | grep "deepseek" -A 1 | grep "api-key:" | awk '{print $2}')

if [ -z "$CONFIG_API_KEY" ]; then
    # 尝试另一种方式读取
    CONFIG_API_KEY=$(awk '/deepseek:/,/api-key:/' src/main/resources/application-dev.yml | grep "api-key:" | awk '{print $2}')
fi

if [ -n "$CONFIG_API_KEY" ]; then
    echo "   使用配置文件中的 API Key 进行测试..."

    RESPONSE=$(curl -s -w "\n%{http_code}" \
        -X POST https://api.deepseek.com/chat/completions \
        -H "Authorization: Bearer $CONFIG_API_KEY" \
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
        echo "   响应预览: $(echo $BODY | head -c 100)..."
    elif [ "$HTTP_CODE" = "401" ]; then
        echo "   ❌ API Key 无效或已过期 (HTTP $HTTP_CODE)"
    elif [ "$HTTP_CODE" = "429" ]; then
        echo "   ⚠️  请求频率限制 (HTTP $HTTP_CODE)"
    else
        echo "   ❌ API 连接失败 (HTTP $HTTP_CODE)"
        echo "   响应: $(echo $BODY | head -c 200)"
    fi
else
    echo "   ❌ 无法从配置文件读取 API Key"
fi
echo ""

# 6. 总结
echo "========================================="
echo "📊 诊断总结"
echo "========================================="
echo ""

if grep -q "active: local,dev" src/main/resources/application.yml 2>/dev/null; then
    echo "✅ 应用配置了正确的 profiles: local,dev"
else
    echo "⚠️  配置文件可能未正确设置"
fi

if [ -f src/main/resources/application-dev.yml ]; then
    echo "✅ application-dev.yml 存在"
else
    echo "❌ application-dev.yml 不存在"
fi

if [ -n "$CONFIG_API_KEY" ]; then
    echo "✅ DeepSeek API Key 已配置"
else
    echo "❌ DeepSeek API Key 未找到"
fi

echo ""
echo "========================================="
echo "🚀 下一步"
echo "========================================="
echo ""
echo "如果 API Key 测试失败，请检查:"
echo "1. API Key 是否有效"
echo "2. 账户余额是否充足"
echo "3. 是否需要访问白名单"
echo ""
echo "如果 API Key 测试成功，请重启应用:"
echo "mvn spring-boot:run"
echo ""
