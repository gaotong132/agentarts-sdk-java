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
| `--host` | `-H` | 绑定地址 | `0.0.0.0` |
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

# 设置非敏感环境变量
agentarts dev --env LOG_LEVEL=DEBUG --env MODEL_NAME=gpt-4o

# 简写参数
agentarts dev -p 3000 -e KEY1=value1 -e KEY2=value2

# 指定配置文件
agentarts dev --config /path/to/config.yaml
```

## 工作原理

`dev` 从 `.agentarts_config.yaml` 读取 `base.entrypoint`，先用 Maven 编译项目并生成运行时类路径，再在受管子 JVM 中加载入口类并调用 `public static AgentArtsRuntimeApp createApp()`。子进程继承真实环境变量，退出、重载和 CLI 中断时都会执行进程树清理。

启用 `--reload` 后，源码、资源、POM、已编译类和配置发生变化会触发去抖动重编译与子进程重启；编译失败时保留当前健康进程。单次构建最长 10 分钟，避免开发命令永久挂起。

若入口类不存在、签名不正确、工厂返回 `null` 或子进程异常退出，`dev` 会返回非零状态并保留原因，不会静默回退到 echo Agent。

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

1. `--env` 命令行参数（最高优先级）
2. `.agentarts_config.yaml` 中当前 Agent 的环境变量
3. 启动 CLI 的系统环境变量

> 不要通过 `--env` 传递密钥：命令行可能进入 shell 历史或进程列表。敏感值应在启动 CLI 前注入当前进程环境，或由 CI Secret 管理。
