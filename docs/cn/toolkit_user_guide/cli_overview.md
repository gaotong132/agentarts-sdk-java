# agentarts CLI — 安装与前置步骤

`agentarts` 是基于 Java 的命令行工具，并非系统自带命令。终端直接输入 `agentarts` 不会生效，需先构建 jar 并将启动器加入 PATH。本文说明前置步骤。

## 1. 环境要求

| 项 | 要求 |
|---|---|
| JDK | 17 及以上（构建与运行均为 Java 17 字节码） |
| Maven | 3.9 及以上（仅构建时需要） |
| Docker | 仅 `deploy` 云部署链需要 |

确认环境：

```bash
java -version    # 17 及以上
mvn -version     # 3.9 及以上（仅构建需要）
```

Windows PowerShell：

```powershell
java -version    # 17 及以上
mvn -version     # 3.9 及以上（仅构建需要）
```

> Windows 上 `mvn` 实为 `mvn.cmd`（cmd 脚本）。在 PowerShell 与 Git Bash 中均可直接调用 `mvn`；若从某些工具链调用报 "CreateProcess error=193"，请确认调用的是 `mvn.cmd` 而非无扩展名的 bash 脚本。

## 2. 构建 CLI jar

在仓库根目录执行（首次构建会编译全部上游模块）：

```bash
mvn -pl agentarts-toolkit-cli package -am -DskipTests
```

产物为两个 jar：

```
agentarts-toolkit-cli/target/agentarts-toolkit-cli-0.1.0-SNAPSHOT.jar            # thin jar，供其它模块依赖
agentarts-toolkit-cli/target/agentarts-toolkit-cli-0.1.0-SNAPSHOT-standalone.jar # 自包含 fat jar，CLI 运行用
```

`-standalone` 为自包含 fat jar，picocli 与 SDK 依赖均已打入，`java -jar` 无需额外 classpath。该产物由 `maven-shade-plugin` 以 classified 附加产物方式生成，thin 主 jar 保留给依赖本模块的其它模块（如 `agentarts-sdk-tests`）。

## 3. 调用方式

### 方式一：java -jar

```bash
java -jar agentarts-toolkit-cli/target/agentarts-toolkit-cli-0.1.0-SNAPSHOT-standalone.jar --version
# AgentArts CLI 0.1.0
```

### 方式二：bin/agentarts 启动器（推荐）

仓库提供 `bin/agentarts`（bash）与 `bin/agentarts.cmd`（Windows），自动定位 fat jar 并转发参数：

```bash
./bin/agentarts --version
./bin/agentarts --help
```

Windows（PowerShell 与 cmd.exe 均可，`.cmd` 扩展名可省略）：

```powershell
.\bin\agentarts.cmd --version
.\bin\agentarts.cmd --help
```

将其注册为 `agentarts` 命令，二选一：

```bash
# (a) 将 bin 加入 PATH
export PATH="$PWD/bin:$PATH"
agentarts --version

# (b) 或定义 alias / 函数
alias agentarts="java -jar $PWD/agentarts-toolkit-cli/target/agentarts-toolkit-cli-0.1.0-SNAPSHOT-standalone.jar"
```

Windows PowerShell：

```powershell
# (a) 当前会话加入 PATH（仅本会话生效）
$env:PATH = "$PWD\bin;$env:PATH"
agentarts --version

# (b) 持久化（写入用户环境变量，新开终端生效）
setx PATH "$PWD\bin;$env:PATH"
# 注意：setx 会截断超过 1024 字符的 PATH，建议改用「系统属性 → 环境变量」图形界面编辑。

# (c) 或定义函数（写入 $PROFILE 可跨会话复用）
function agentarts { java -jar "$PWD\agentarts-toolkit-cli\target\agentarts-toolkit-cli-0.1.0-SNAPSHOT-standalone.jar" @args }
```

> Windows 下 `bin/agentarts.cmd` 由 cmd 解释器运行，在 PowerShell 与 cmd.exe 中均可直接调用 `agentarts`。若提示找不到命令，确认 `bin` 已在 PATH 中、且 `.cmd` 扩展名未被 `PATHEXT` 排除（默认包含 `.CMD`）。

启动器查找 jar 的顺序：`AGENTARTS_CLI_JAR` 环境变量 → 脚本同目录的 `agentarts-toolkit-cli.jar` → 仓库内 `agentarts-toolkit-cli/target/...-standalone.jar`。将 jar 复制到 `bin/` 并改名为 `agentarts-toolkit-cli.jar`，即可脱离源码树使用。

### 方式三：mvn exec:java（开发期）

修改源码后无需 `package` 即可运行：

```bash
mvn -pl agentarts-toolkit-cli exec:java \
  -Dexec.mainClass=com.huaweicloud.agentarts.toolkit.AgentArtsCli \
  -Dexec.args="--version"
```

Windows PowerShell（续行符用反引号 `` ` ``）：

```powershell
mvn -pl agentarts-toolkit-cli exec:java `
  "-Dexec.mainClass=com.huaweicloud.agentarts.toolkit.AgentArtsCli" `
  "-Dexec.args=--version"
```

## 4. 验证

```bash
agentarts --version     # AgentArts CLI 0.1.0
agentarts --help        # 列出 9 个顶层命令：init / config / dev / deploy / invoke / destroy / runtime / mcp-gateway / memory
```

输出版本号与命令列表即安装成功。

### 4.1 Windows PowerShell 引号注意事项（重要）

Windows PowerShell 5.1 把参数传给原生命令（`curl.exe`、`java`）以及 `agentarts` 的 `.cmd` 启动器时，会**吞掉字符串里的双引号**。因此凡是命令里带 JSON 字面量（如 `'{"message":"Hello!"}'`）的写法，在 PS 5.1 下都会被对端收成 `{message:Hello!}`，触发 `400 Invalid JSON`。

**规避方法（任选其一）**：

1. **`--%` 停止解析 + `\"` 转义**（PS 5.1 通用解法）。`--%` 让 PowerShell 不再解析其后内容，JSON 内层双引号写成 `\"`：

   ```powershell
   agentarts --% invoke "{\"message\":\"Hello!\"}" -a my-agent
   curl.exe --% -X POST http://localhost:8080/invocations -H "Content-Type: application/json" -d "{\"message\": \"Hello!\"}"
   ```

   > `--%` 之后整行不再做 PowerShell 解析，故无法用 `$VAR` 变量插值，也无法用反引号续行——含 JSON 的命令写成单行。

2. **升级 PowerShell 7（`pwsh`）**。PS 7 默认启用正确的原生参数传递（`$PSNativeCommandArgumentPassing`），`'{"message":"Hello!"}'` 可直接使用，变量插值/续行正常。

3. **HTTP 场景改用 `Invoke-RestMethod`**（原生 cmdlet，不经原生参数传递层，无引号坑）：

   ```powershell
   Invoke-RestMethod -Uri http://localhost:8080/invocations -Method Post -ContentType "application/json" -Body '{"message": "Hello!"}'
   ```

> 不含 JSON 的命令（纯路径、带空格的命令串如 `"python process.py data.csv"`、`$VAR` 变量）在 PS 5.1 下正常，无需 `--%`。仅当位置参数或选项值里带双引号字面量时才需要上述处理。

## 5. 云命令的额外前置

`init` / `config` / `dev` 为纯本地命令，无需凭证。涉及云的命令（`deploy` / `invoke --mode cloud` / `destroy` / `runtime *` / `mcp-gateway *` / `memory *`）需设置 AK/SK：

```bash
export HUAWEICLOUD_SDK_AK="your-access-key"
export HUAWEICLOUD_SDK_SK="your-secret-key"
export HUAWEICLOUD_SDK_REGION="cn-southwest-2"   # 可选，默认 cn-southwest-2
```

Windows PowerShell：

```powershell
# 当前会话（仅本会话生效）
$env:HUAWEICLOUD_SDK_AK = "your-access-key"
$env:HUAWEICLOUD_SDK_SK = "your-secret-key"
$env:HUAWEICLOUD_SDK_REGION = "cn-southwest-2"   # 可选，默认 cn-southwest-2

# 持久化（写入用户环境变量，新开终端生效）
setx HUAWEICLOUD_SDK_AK "your-access-key"
setx HUAWEICLOUD_SDK_SK "your-secret-key"
setx HUAWEICLOUD_SDK_REGION "cn-southwest-2"
```

Windows cmd.exe：

```cmd
set HUAWEICLOUD_SDK_AK=your-access-key
set HUAWEICLOUD_SDK_SK=your-secret-key
set HUAWEICLOUD_SDK_REGION=cn-southwest-2
```

凭证仅通过环境变量注入，不得写入配置文件或提交仓库。数据面 Memory 与 Code Interpreter 另需各自的 API Key 环境变量，详见对应子命令文档。

## 6. 子命令文档

| 命令 | 文档 |
|---|---|
| `init` | [init.md](init.md) |
| `config` | [config.md](config.md) |
| `dev` | [dev.md](dev.md) |
| `deploy` | [deploy.md](deploy.md) |
| `invoke` | [invoke.md](invoke.md) |
| `destroy` | [destroy.md](destroy.md) |
| `runtime` | [runtime_cli.md](runtime_cli.md) |
| `mcp-gateway` | [mcp_gateway_cli.md](mcp_gateway_cli.md) |

典型流程：`init` → `config` → `dev`（本地验证）→ `deploy`（云端）→ `invoke`。
