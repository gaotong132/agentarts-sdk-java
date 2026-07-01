# agentarts invoke — 调用 Agent

`invoke` 命令向已部署的 Agent 发送调用请求。

## 用法

```bash
agentarts invoke <payload> [选项]
```

## 参数

| 参数 | 缩写 | 说明 | 默认值 |
|------|------|------|--------|
| `payload` | | JSON 请求体 | 必需 |
| `--agent` | `-a` | Agent 名称 | 默认 Agent |
| `--region` | `-r` | 华为云区域 | 配置文件值 |
| `--mode` | `-m` | 调用模式：`cloud` 或 `local` | `cloud` |
| `--port` | `-p` | 本地模式端口 | `8080` |
| `--endpoint` | | 自定义端点 URL | 自动检测 |
| `--session` | | 会话 ID | — |
| `--bearer-token` | | Bearer 认证令牌 | — |
| `--timeout` | | 超时时间（秒） | `30` |
| `--user-id` | `-u` | 用户 ID | — |

## 调用模式

### cloud — 调用云端 Agent

```bash
agentarts invoke '{"message":"Hello!"}' -a my-agent
agentarts invoke '{"message":"Hello!"}' -a my-agent -r cn-southwest-2
```

### local — 调用本地 Agent

```bash
agentarts invoke '{"message":"Hello!"}' -m local
agentarts invoke '{"message":"Hello!"}' -m local -p 3000
```

## 示例

```bash
# 基本调用
agentarts invoke '{"message":"Hello!"}'

# 指定 Agent 和区域
agentarts invoke '{"message":"Hello!"}' -a my-agent -r cn-southwest-2

# 本地调用
agentarts invoke '{"message":"Hello!"}' -m local -p 8080

# 带会话 ID
agentarts invoke '{"message":"继续上次对话"}' -a my-agent --session session-123

# 带 Bearer 认证
agentarts invoke '{"message":"Hello!"}' -a my-agent --bearer-token your-token

# 自定义端点
agentarts invoke '{"message":"Hello!"}' --endpoint https://custom-endpoint.com

# 设置超时
agentarts invoke '{"message":"Complex task"}' --timeout 120

# 带用户 ID
agentarts invoke '{"message":"Hello!"}' -u user-123

# 复杂 JSON
agentarts invoke '{"message":"Calculate","data":[1,2,3],"options":{"verbose":true}}'
```

## Payload 格式

基本格式：

```json
{"message": "your message here"}
```

扩展格式：

```json
{
  "message": "your message",
  "session_id": "optional-session-id",
  "data": {"key": "value"},
  "stream": false
}
```

## 会话管理

使用 `--session` 保持对话连续性：

```bash
# 第一次调用（自动创建 session）
agentarts invoke '{"message":"Hello!"}' -a my-agent

# 后续调用（使用返回的 session_id）
agentarts invoke '{"message":"What did I say?"}' -a my-agent --session returned-session-id
```
