# agentarts invoke — 调用 Agent

`invoke` 命令向已部署的 Agent 发送调用请求。

## 用法

```bash
agentarts invoke <payload> [选项]
```

## 参数

| 参数 | 缩写 | 说明 | 默认值 |
|------|------|------|--------|
| `payload` | | JSON 请求体（位置参数） | 必需 |
| `--agent` | `-a` | Agent 名称 | 默认 Agent |
| `--region` | `-r` | 华为云区域（cloud 模式） | 配置文件值 |
| `--mode` | `-m` | 调用模式：`cloud` 或 `local` | `cloud` |
| `--port` | `-p` | 本地模式端口 | `8080` |
| `--endpoint` | `-e` | Endpoint 名称（非 URL） | 自动检测 |
| `--session` | `-s` | 会话 ID | — |
| `--bearer-token` | `-bt` | Bearer 认证令牌 | — |
| `--timeout` | | 请求超时（秒） | `900` |
| `--user-id` | `-u` | 用户 ID（OAuth2 出站凭证） | — |
| `--skip-ssl-verification` | `-k` | 跳过 SSL 证书验证 | `false` |
| `--custom-path` | | 附加到 `/invocations` 之后的自定义路径 | — |

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
agentarts invoke '{"message":"Hello!"}' -a my-agent -bt your-token

# 指定 Endpoint 名称
agentarts invoke '{"message":"Hello!"}' -a my-agent -e my-endpoint

# 设置超时
agentarts invoke '{"message":"Complex task"}' --timeout 120

# 带用户 ID
agentarts invoke '{"message":"Hello!"}' -u user-123

# 复杂 JSON
agentarts invoke '{"message":"Calculate","data":[1,2,3],"options":{"verbose":true}}'
```

### Windows PowerShell

> PS 5.1 把 JSON 里的双引号传给 `agentarts`（`.cmd`→`java`）时会吞掉，`'{"message":"Hello!"}'` 会被收成 `{message:Hello!}` 触发 400。含 JSON 的命令需用 `--%` 停止解析 + `\"` 转义内层双引号，且写成单行（详见 [cli_overview.md §4.1](cli_overview.md#41-windows-powershell-引号注意事项重要)）：

```powershell
# 基本调用
agentarts --% invoke "{\"message\":\"Hello!\"}" -a my-agent

# 本地调用
agentarts --% invoke "{\"message\":\"Hello!\"}" -m local -p 8080

# 带会话 ID
agentarts --% invoke "{\"message\":\"继续上次对话\"}" -a my-agent --session session-123

# 复杂 JSON
agentarts --% invoke "{\"message\":\"Calculate\",\"data\":[1,2,3],\"options\":{\"verbose\":true}}"
```

> 升级 PowerShell 7（`pwsh`）后 `'{"message":"Hello!"}'` 可直接使用、支持 `$VAR` 变量与反引号续行；HTTP 场景也可改用 `Invoke-RestMethod`（见 [dev.md](dev.md#测试)）。

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
