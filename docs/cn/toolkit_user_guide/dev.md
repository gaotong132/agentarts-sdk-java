# agentarts dev — 本地开发服务器

`dev` 命令在本地启动 AgentArts Runtime 开发服务器。

## 用法

```bash
agentarts dev [选项]
```

## 参数

| 参数 | 缩写 | 说明 | 默认值 |
|------|------|------|--------|
| `--port` | `-p` | 服务器端口 | `8080` |
| `--host` | `-h` | 绑定地址 | `0.0.0.0` |
| `--reload` | | 启用热重载 | `false` |
| `--config` | `-c` | 配置文件路径 | `.agentarts_config.yaml` |
| `--path` | | 项目路径（含 `.agentarts_config.yaml`） | 当前目录 |
| `--env` | `-e` | 设置环境变量（`KEY=VALUE`，可多次使用） | — |

## 示例

```bash
# 基本启动
agentarts dev

# 自定义端口
agentarts dev --port 3000

# 启用热重载
agentarts dev --reload

# 设置环境变量
agentarts dev --env OPENAI_API_KEY=sk-xxx --env MODEL_NAME=gpt-4o

# 简写参数
agentarts dev -p 3000 -e KEY1=value1 -e KEY2=value2

# 指定配置文件
agentarts dev --config /path/to/config.yaml
```

## 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/invocations` | POST | Agent 调用入口 |
| `/ping` | GET | 健康检查 |

## 测试

```bash
# 健康检查
curl http://localhost:8080/ping

# 调用 Agent
curl -X POST http://localhost:8080/invocations \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello!"}'
```

Windows PowerShell：

```powershell
# PowerShell 里 curl 是 Invoke-WebRequest 的别名，不支持 -H/-d 语法。用 curl.exe 或 Invoke-RestMethod。

# 方式一（推荐）：Invoke-RestMethod
Invoke-RestMethod -Uri http://localhost:8080/ping
Invoke-RestMethod -Uri http://localhost:8080/invocations -Method Post `
  -ContentType "application/json" -Body '{"message": "Hello!"}'

# 方式二：curl.exe（PS 5.1 带 JSON 需 --% + \"，见 cli_overview.md §4.1）
curl.exe http://localhost:8080/ping
curl.exe --% -X POST http://localhost:8080/invocations -H "Content-Type: application/json" -d "{\"message\": \"Hello!\"}"
```

> 请求体是一个任意 JSON 对象，整体作为 `payload` 传给 Agent 的 entrypoint；字段名（如 `message`）取决于 entrypoint 代码读取的 key。

## 环境变量优先级

1. `--env` 命令行参数（通过 `System.setProperty` 注入，最高优先级）
2. 系统环境变量

> `dev` 命令从配置文件读取 entrypoint 与端口等，但当前不自动应用配置文件中的 `runtime.environment_variables`；如需注入环境变量，请用 `--env` 或系统环境变量。
