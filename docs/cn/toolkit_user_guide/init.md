# agentarts init — 初始化项目

`init` 命令用于创建新的 AgentArts Agent 项目骨架。

## 用法

```bash
agentarts init [选项]
```

## 参数

| 参数 | 缩写 | 说明 | 默认值 |
|------|------|------|--------|
| `--name` | `-n` | 项目名称 | 省略时交互式提示输入 |
| `--template` | `-t` | 项目模板 | `basic` |
| `--path` | | 项目路径 | 当前目录 |
| `--region` | `-r` | 华为云区域 | `cn-southwest-2` |
| `--swr-org` | | SWR 组织名 | 项目名 |
| `--swr-repo` | | SWR 仓库名 | 项目名 |

## 模板类型

| 模板 | 说明 |
|------|------|
| `basic` | 基础 Java 处理器 |
| `agentscope` | agentscope-java ReActAgent 集成 |

> Dockerfile 在所有模板下都会生成（`docker/Dockerfile.tpl`），无需单独的 `docker` 模板。

## 示例

```bash
# 交互式初始化：提示输入项目名，其余参数取默认值
agentarts init
# Project name: my-agent

# 快速创建（跳过交互）
agentarts init -n my-agent -t basic -r cn-southwest-2

# 指定完整参数
agentarts init --name my-agent --template agentscope --region cn-southwest-2 --swr-org my-org --swr-repo my-repo
```

> `agentarts init` 不带 `--name` 时进入交互模式：在终端提示输入项目名。若输入为空或按 Ctrl-D，命令以非零退出码结束且不创建任何文件。非交互环境（管道输入、CI、IDE 测试桩等无 TTY）下不会提示，直接报 `--name is required`，需显式传 `-n`。

## 生成的项目结构

```
my-agent/
├── pom.xml                          # Maven 构建文件
├── Dockerfile                       # 容器镜像构建文件
├── src/main/java/com/example/
│   └── Agent.java                   # Agent 入口类（public class Agent，与文件名一致）
└── .agentarts_config.yaml           # AgentArts 配置文件
```

## 后续步骤

```bash
cd my-agent
mvn compile
agentarts dev        # 本地开发
agentarts deploy     # 云端部署
```
