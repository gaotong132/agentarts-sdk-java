# agentarts CLI — 安装与前置步骤

`agentarts` 是一个基于 Java 的命令行工具。**直接在终端敲 `agentarts` 不会生效**——它不是系统自带命令，需要先构建 jar 并把启动器暴露到 PATH。本文档说明前置步骤。

## 1. 环境要求

| 项 | 运行 CLI | 从源码构建 |
|---|---|---|
| JDK | 17+ | 26（见 `CLAUDE.md`，系统 Maven + JDK 26） |
| Maven | 不需要（运行 jar） | 3.9+ |
| Docker | 仅 `deploy` 云部署链需要 | 同左 |

> 产物 jar 的字节码目标是 Java 17，因此运行只需 JDK 17+；但**构建**本 SDK 需按 `CLAUDE.md` 用 JDK 26 + 系统 Maven（`D:\apache-maven-3.9.16`）。

## 2. 构建 CLI jar

在仓库根目录执行（首次构建会编译全部上游模块）：

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-26.0.1"
export PATH="$JAVA_HOME/bin:/d/apache-maven-3.9.16/bin:$PATH"

mvn -pl agentarts-toolkit-cli package -am -DskipTests
```

产物为两个 jar：

```
agentarts-toolkit-cli/target/agentarts-toolkit-cli-0.1.0-SNAPSHOT.jar            # thin jar，供其它模块依赖
agentarts-toolkit-cli/target/agentarts-toolkit-cli-0.1.0-SNAPSHOT-standalone.jar # 自包含 fat jar，CLI 运行用
```

`-standalone` 是**自包含 fat jar**（picocli + SDK 依赖已打入，`java -jar` 无需额外 classpath）。`maven-shade-plugin` 以 classified 附加产物方式生成它，thin 主 jar 保留给依赖本模块的其它模块（如 `agentarts-sdk-tests`）。

> 若只想跑一次而不留 jar，见 §3 方式三。

## 3. 三种调用方式

### 方式一：`java -jar`（最直接）

```bash
java -jar agentarts-toolkit-cli/target/agentarts-toolkit-cli-0.1.0-SNAPSHOT-standalone.jar --version
# AgentArts CLI 0.1.0
```

### 方式二：`bin/agentarts` 包装脚本（推荐，日常使用）

仓库提供 `bin/agentarts`（bash）与 `bin/agentarts.cmd`（Windows）。脚本自动定位 fat jar 并转发参数：

```bash
./bin/agentarts --version      # AgentArts CLI 0.1.0
./bin/agentarts --help         # 列出全部子命令
```

把它变成真正的 `agentarts` 命令，二选一：

```bash
# (a) 把 bin/ 加到 PATH（持久化写进 ~/.bashrc）
export PATH="$PWD/bin:$PATH"
agentarts --version

# (b) 或加一个 alias / 函数
alias agentarts="java -jar $PWD/agentarts-toolkit-cli/target/agentarts-toolkit-cli-0.1.0-SNAPSHOT-standalone.jar"
```

Windows PowerShell：

```powershell
$env:PATH = "$PWD\bin;$env:PATH"
agentarts --version
```

> 脚本查找 jar 的顺序：`AGENTARTS_CLI_JAR` 环境变量 → 脚本旁的 `agentarts-toolkit-cli.jar` → 仓库内 `agentarts-toolkit-cli/target/...`。把 jar 复制到 `bin/` 改名即可脱离源码树使用。

### 方式三：`mvn exec:java`（开发期，不产 jar）

改了源码想立即试，无需 `package`：

```bash
mvn -pl agentarts-toolkit-cli exec:java \
  -Dexec.mainClass=com.huaweicloud.agentarts.toolkit.AgentArtsCli \
  -Dexec.args="--version"
```

子命令参数跟在 `--` 后：

```bash
mvn -pl agentarts-toolkit-cli exec:java \
  -Dexec.mainClass=com.huaweicloud.agentarts.toolkit.AgentArtsCli \
  -Dexec.args="--help"
```

## 4. 验证安装

```bash
agentarts --version     # AgentArts CLI 0.1.0
agentarts --help        # 打印 9 个顶层命令：init / config / dev / deploy / invoke / destroy / runtime / mcp-gateway / memory
```

能看到版本号与命令列表即安装成功。

## 5. 云命令的额外前置

`init` / `config` / `dev` 是纯本地命令，无需凭证。涉及云的命令（`deploy` / `invoke --mode cloud` / `destroy` / `runtime *` / `mcp-gateway *` / `memory *`）需先设置 AK/SK：

```bash
export HUAWEICLOUD_SDK_AK="your-access-key"
export HUAWEICLOUD_SDK_SK="your-secret-key"
export HUAWEICLOUD_SDK_REGION="cn-southwest-2"   # 可选，默认 cn-southwest-2
```

> 凭证**只通过环境变量注入**，绝不写入配置文件或提交仓库（见 [凭证安全约定](../e2e_testing_guide.md#7-凭证安全)）。数据面 Memory/Code Interpreter 另需各自的 API Key 环境变量，详见对应子命令文档。

## 6. 下一步

按子命令文档操作：

| 命令 | 文档 |
|---|---|
| `init` — 初始化项目 | [init.md](init.md) |
| `config` — 配置管理 | [config.md](config.md) |
| `dev` — 本地开发服务器 | [dev.md](dev.md) |
| `deploy` — 部署 | [deploy.md](deploy.md) |
| `invoke` — 调用 Agent | [invoke.md](invoke.md) |
| `destroy` — 销毁 Agent | [destroy.md](destroy.md) |
| `runtime` — 运行时管理 | [runtime_cli.md](runtime_cli.md) |
| `mcp-gateway` — MCP 网关 | [mcp_gateway_cli.md](mcp_gateway_cli.md) |

典型流程：`init` → `config` → `dev`（本地验证）→ `deploy`（云端）→ `invoke`。
