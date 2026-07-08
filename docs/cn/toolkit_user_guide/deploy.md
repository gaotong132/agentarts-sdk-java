# agentarts deploy — 部署 Agent

`deploy` 命令（别名 `launch`）将 Agent 部署到华为云或本地环境。

## 用法

```bash
agentarts deploy [选项]
agentarts launch [选项]  # 别名
```

## 参数

| 参数 | 缩写 | 说明 | 默认值 |
|------|------|------|--------|
| `--agent` | `-a` | Agent 名称 | 默认 Agent |
| `--mode` | `-m` | 部署模式：`cloud` 或 `local` | `cloud` |
| `--tag` | `-t` | Docker 镜像标签 | `latest` |
| `--local-port` | `-l` | 本地模式端口映射 | — |
| `--swr-org` | | SWR 组织名（覆盖配置文件） | 配置文件值 |
| `--swr-repo` | | SWR 仓库名（覆盖配置文件） | 配置文件值 |
| `--description` | `-d` | Agent 描述 | — |
| `--skip-build` | | 跳过构建/推送，直接用配置中的镜像 URL | `false` |
| `--skip-ssl-verification` | `-k` | 跳过 SSL 证书验证 | `false` |

## 部署模式

### cloud — 云端部署

```bash
agentarts deploy -a my-agent -m cloud -t v1.0
```

部署流程：
1. Docker 构建镜像
2. SWR 创建命名空间/仓库
3. 推送镜像到 SWR
4. 创建 AgentArts Runtime
5. 返回 Runtime 端点

### local — 本地部署

```bash
agentarts deploy -a my-agent -m local --local-port 8080
```

## 示例

> 多行示例使用 bash 反斜杠 `\` 续行；Windows PowerShell 下改为反引号 `` ` ``，或写成一行。

```bash
# 基本部署（云端）
agentarts deploy

# 指定 Agent 和标签
agentarts deploy --agent my-agent --tag v1.0.0

# 本地部署
agentarts deploy --mode local --local-port 3000

# 指定 SWR
agentarts deploy --swr-org my-org --swr-repo my-repo

# 跳过构建（使用已有镜像）
agentarts deploy --skip-build

# 完整参数
agentarts deploy -a my-agent -m cloud -t v2.0 --swr-org my-org --swr-repo my-repo \
  -d "Production deployment v2.0"
```

```powershell
# 完整参数
agentarts deploy -a my-agent -m cloud -t v2.0 --swr-org my-org --swr-repo my-repo `
  -d "Production deployment v2.0"
```

## 部署前准备

确保配置文件正确：

```yaml
# .agentarts_config.yaml
agents:
  my-agent:
    base:
      name: my-agent
      entrypoint: com.example.MyAgent
      region: cn-southwest-2
    swr_config:
      organization: my-org
      repository: my-repo
    runtime:
      invoke_config:
        protocol: HTTP
        port: 8080
```

## 常见问题

### 部署失败？

1. 检查 AK/SK 权限
2. 确认 SWR 组织/仓库存在
3. 检查网络连接

### 镜像 URL 优先级

1. 配置文件中的 `runtime.artifact_source.url`（最高，跳过 SWR 推送）
2. `--swr-org` + `--swr-repo` 命令行参数
3. 配置文件中的 `swr_config`

> 若以上均未提供，deploy 将报错（不会自动回退到 Agent 名称）。
