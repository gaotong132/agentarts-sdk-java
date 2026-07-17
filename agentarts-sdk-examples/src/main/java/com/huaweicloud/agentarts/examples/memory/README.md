# Memory Service Example

展示如何使用 AgentArts Memory Service 存储对话历史。

示例在 Runtime 停机时关闭 `MemoryClient`。生产应用应复用客户端，避免每个请求新建连接池。

## 快速开始

```bash
# 设置环境变量
export HUAWEICLOUD_SDK_MEMORY_API_KEY="your-api-key"
export AGENTARTS_MEMORY_SPACE_ID="your-space-id"

# 运行 Agent
mvn compile exec:java -pl agentarts-sdk-examples \
  -Dexec.mainClass="com.huaweicloud.agentarts.examples.memory.MemoryUsageExample"
```

## 测试

```bash
# 发送消息（会自动创建 session）
curl -X POST http://localhost:8080/invocations \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello!"}'

# 使用相同 session 继续对话
curl -X POST http://localhost:8080/invocations \
  -H "Content-Type: application/json" \
  -d '{"message": "What did I say before?", "session_id": "xxx"}'
```

## 请求参数

| 参数 | 说明 | 必需 |
|------|------|------|
| `message` | 用户消息 | 是 |
| `session_id` | 会话 ID（不传则自动创建） | 否 |
| `space_id` | Memory Space ID（或设置环境变量） | 是 |

## 环境变量

| 变量名 | 说明 | 必需 |
|--------|------|------|
| `HUAWEICLOUD_SDK_MEMORY_API_KEY` | Memory Service API Key | 是 |
| `HUAWEICLOUD_SDK_REGION` | 华为云区域 | 否（默认 cn-southwest-2） |
| `AGENTARTS_MEMORY_SPACE_ID` | Memory Space ID | 是（或在请求中传递） |

API Key 仅在当前进程环境或 Secret Manager 中提供，不要写入源码、请求 JSON、配置文件或日志。服务端响应中的加密消息内容不应被当作明文解析。
