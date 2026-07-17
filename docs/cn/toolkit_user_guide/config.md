# agentarts config — 配置管理

`config` 命令管理 `.agentarts_config.yaml` 配置文件，支持查看、设置、删除配置项和环境变量。

## 子命令

| 子命令 | 说明 |
|--------|------|
| `config` | 交互式配置 / 快速添加 Agent |
| `config list` | 列出所有 Agent |
| `config get <key>` | 获取配置值 |
| `config set <key> <value>` | 设置配置值 |
| `config set-default <name>` | 设置默认 Agent |
| `config remove <name>` | 删除 Agent |
| `config set-env <key> <value>` | 设置环境变量 |
| `config remove-env <key>` | 删除环境变量 |
| `config list-env` | 列出环境变量 |

## config — 添加 Agent

```bash
# 交互式配置
agentarts config

# 快速配置
agentarts config -n my-agent -e com.example.MyAgent -r cn-southwest-2

# 完整参数
agentarts config --name my-agent --entrypoint com.example.MyAgent --region cn-southwest-2 \
  --dependency-file pom.xml --swr-org my-org --swr-repo my-repo
```

Windows PowerShell（行尾续行符用反引号 `` ` ``，或直接写成一行）：

```powershell
# 完整参数
agentarts config --name my-agent --entrypoint com.example.MyAgent --region cn-southwest-2 `
  --dependency-file pom.xml --swr-org my-org --swr-repo my-repo
```

## config list — 列出所有 Agent

```bash
agentarts config list
# 输出：
#   my-agent *
#   another-agent
```

`*` 标记默认 Agent。

## config get — 获取配置值

```bash
agentarts config get base.region
agentarts config get base.entrypoint --agent my-agent
```

## config set — 设置配置值

```bash
agentarts config set base.region cn-north-4
agentarts config set base.region cn-north-4 --agent my-agent
```

### 可用配置键

| 键 | 说明 |
|---|---|
| `base.name` | Agent 名称 |
| `base.entrypoint` | Java 入口类 |
| `base.region` | 华为云区域 |
| `base.dependency_file` | 依赖文件 |
| `swr_config.organization` | SWR 组织名 |
| `swr_config.repository` | SWR 仓库名 |
| `runtime.invoke_config.protocol` | 协议（HTTP） |
| `runtime.invoke_config.port` | 端口 |

## config set-default — 设置默认 Agent

```bash
agentarts config set-default my-agent
```

## config remove — 删除 Agent

```bash
agentarts config remove my-agent
```

## config set-env — 设置环境变量

```bash
agentarts config set-env LOG_LEVEL INFO
agentarts config set-env MODEL_NAME gpt-4o --agent my-agent
```

## config list-env — 列出环境变量

```bash
agentarts config list-env
agentarts config list-env --agent my-agent
```

## config remove-env — 删除环境变量

```bash
agentarts config remove-env LOG_LEVEL
agentarts config remove-env MODEL_NAME --agent my-agent
```

## 配置文件格式

`.agentarts_config.yaml` 完整示例：

```yaml
default_agent: my-agent
agents:
  my-agent:
    base:
      name: my-agent
      entrypoint: com.example.MyAgent
      region: cn-southwest-2
      language: java17
      dependency_file: pom.xml
    swr_config:
      organization: my-org
      repository: my-repo
      organization_auto_create: false
      repository_auto_create: false
    runtime:
      invoke_config:
        protocol: HTTP
        port: 8080
        url_match_type: ACCURATE_MATCH
      environment_variables:
        LOG_LEVEL: INFO
        MODEL_NAME: gpt-4o
```

配置更新采用临时文件加原子替换，避免中途失败留下半写文件；`get`/`list-env` 对环境变量值统一显示 `[REDACTED]`。配置文件仍不是 Secret 存储：AK/SK、API Key、OAuth2 Secret 等敏感值不得通过 `set-env` 持久化，应由进程环境或 CI Secret 注入。
