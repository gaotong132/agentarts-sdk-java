# agentarts init — 初始化项目

`init` 命令用于创建新的 AgentArts Agent 项目骨架。

## 用法

```bash
agentarts init [选项]
```

## 参数

| 参数 | 缩写 | 说明 | 默认值 |
|------|------|------|--------|
| `--name` | `-n` | 项目名称 | 交互式输入 |
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
| `docker` | Docker 部署模板 |

## 示例

```bash
# 交互式初始化
agentarts init

# 快速创建
agentarts init -n my-agent -t basic -r cn-southwest-2

# 指定完整参数
agentarts init --name my-agent --template agentscope --region cn-southwest-2 --swr-org my-org --swr-repo my-repo
```

## 生成的项目结构

```
my-agent/
├── pom.xml                          # Maven 构建文件
├── src/main/java/com/example/
│   └── MyAgentAgent.java            # Agent 入口类
└── .agentarts_config.yaml           # AgentArts 配置文件
```

## 后续步骤

```bash
cd my-agent
mvn compile
agentarts dev        # 本地开发
agentarts deploy     # 云端部署
```
