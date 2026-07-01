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
| `--host` | | 绑定地址 | `0.0.0.0` |
| `--reload` | | 启用热重载 | `false` |
| `--config` | | 配置文件路径 | `.agentarts_config.yaml` |
| `--env` | `-e` | 设置环境变量（可多次使用） | — |

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

## 环境变量优先级

1. `--env` 命令行参数（最高优先级）
2. 配置文件中的 `runtime.environment_variables`
3. 系统环境变量（最低优先级）
