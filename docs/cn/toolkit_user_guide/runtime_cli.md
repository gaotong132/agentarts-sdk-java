# agentarts runtime — 运行时管理

`runtime` 命令提供 Agent 运行时的会话管理、文件传输和命令执行能力。

> 下方多行示例使用 bash 的反斜杠 `\` 续行。Windows PowerShell 下续行符改为反引号 `` ` ``，或写成一行。每处多行示例后均附 PowerShell 版本。
>
> **含 JSON 的命令**（如 `invoke`）在 PS 5.1 下须用 `--%` + `\"` 且单行，见 [cli_overview.md §4.1](cli_overview.md#41-windows-powershell-引号注意事项重要)。

## 子命令

| 子命令 | 说明 |
|--------|------|
| `runtime start-session` | 启动运行时会话 |
| `runtime stop-session` | 关闭运行时会话 |
| `runtime invoke` | 调用 Agent |
| `runtime exec-command` | 执行 Shell 命令 |
| `runtime upload-files` | 上传文件 |
| `runtime download-files` | 下载文件 |

## 通用参数

所有 runtime 子命令支持以下参数：

| 参数 | 缩写 | 说明 |
|------|------|------|
| `--agent` | `-a` | Agent 名称（必填） |
| `--region` | `-r` | 华为云区域 |
| `--endpoint` | `-e` | Endpoint 名称（非 URL） |
| `--bearer-token` | `-bt` | Bearer 认证令牌 |
| `--skip-ssl-verification` | `-k` | 跳过 SSL 验证 |
| `--session` | `-s` | 会话 ID（stop-session / exec-command / upload / download 必填） |
| `--timeout` | | 超时秒数（默认 60–900，视子命令） |

## start-session — 启动会话

```bash
agentarts runtime start-session -a my-agent
agentarts runtime start-session -a my-agent -r cn-southwest-2
agentarts runtime start-session -a my-agent -bt your-token
```

返回会话 ID，用于后续操作。

## stop-session — 关闭会话

```bash
agentarts runtime stop-session -a my-agent -s session-123
```

## invoke — 调用 Agent

`payload` 为位置参数（JSON 字符串，需引号），不是 `--payload` 选项：

```bash
agentarts runtime invoke -a my-agent -s session-123 '{"message":"Hello!"}'

# 自定义路径
agentarts runtime invoke -a my-agent --custom-path /custom/invoke \
  -s session-123 '{"data":"test"}'

# 设置超时
agentarts runtime invoke -a my-agent -s session-123 \
  --timeout 120 '{"message":"Complex task"}'
```

```powershell
# PS 5.1 含 JSON 需用 --% + \"（见 cli_overview.md §4.1）；写成单行
agentarts --% runtime invoke -a my-agent -s session-123 "{\"message\":\"Hello!\"}"

# 自定义路径
agentarts --% runtime invoke -a my-agent --custom-path /custom/invoke -s session-123 "{\"data\":\"test\"}"

# 设置超时
agentarts --% runtime invoke -a my-agent -s session-123 --timeout 120 "{\"message\":\"Complex task\"}"
```

## exec-command — 执行命令

`command` 为位置参数（需引号，整条命令作为一个参数），不是 `--command` 选项：

命令使用服务端 `Command-Type: chunked` 协议，CLI 消费原始 NDJSON 响应并逐行输出，不会先把完整结果聚合到内存。`--timeout` 同时约束 HTTP 调用和流消费。

```bash
# 基本命令执行
agentarts runtime exec-command -a my-agent -s session-123 "ls -la"

# 带超时
agentarts runtime exec-command -a my-agent -s session-123 \
  "python script.py" --timeout 60
```

```powershell
# 基本命令执行
agentarts runtime exec-command -a my-agent -s session-123 "ls -la"

# 带超时
agentarts runtime exec-command -a my-agent -s session-123 `
  "python script.py" --timeout 60
```

## upload-files — 上传文件

单文件最大 64 MiB、单次总量最大 128 MiB；读取前校验普通文件和大小，超限会在网络调用前失败。远程路径与动态资源 ID 经过路径段编码，`..` 等不安全自定义路径会被拒绝。

```bash
# 上传文件到默认目录 (/home/user/)
agentarts runtime upload-files -a my-agent -s session-123 \
  -f file1.txt file2.py

# 上传到指定远程目录
agentarts runtime upload-files -a my-agent -s session-123 \
  -f file1.txt -p /home/user/data/
```

```powershell
# 上传文件到默认目录 (/home/user/)
agentarts runtime upload-files -a my-agent -s session-123 `
  -f file1.txt file2.py

# 上传到指定远程目录
agentarts runtime upload-files -a my-agent -s session-123 `
  -f file1.txt -p /home/user/data/
```

## download-files — 下载文件

`-p/--path` 为远程路径（必填），`-o/--output` 为本地输出路径：

```bash
# 下载单个文件
agentarts runtime download-files -a my-agent -s session-123 \
  -p /home/user/output.txt -o ./output.txt

# 递归下载目录（打包为 tar）
agentarts runtime download-files -a my-agent -s session-123 \
  -p /home/user/logs/ --recursive -o ./logs.tar
```

```powershell
# 下载单个文件
agentarts runtime download-files -a my-agent -s session-123 `
  -p /home/user/output.txt -o ./output.txt

# 递归下载目录（打包为 tar）
agentarts runtime download-files -a my-agent -s session-123 `
  -p /home/user/logs/ --recursive -o ./logs.tar
```

## 完整会话生命周期

```bash
# 1. 启动会话
SESSION_ID=$(agentarts runtime start-session -a my-agent)

# 2. 上传文件
agentarts runtime upload-files -a my-agent -s $SESSION_ID -f data.csv

# 3. 执行命令
agentarts runtime exec-command -a my-agent -s $SESSION_ID \
  "python process.py data.csv"

# 4. 下载结果
agentarts runtime download-files -a my-agent -s $SESSION_ID \
  -p /home/user/result.csv -o ./result.csv

# 5. 关闭会话
agentarts runtime stop-session -a my-agent -s $SESSION_ID
```

```powershell
# 1. 启动会话（捕获输出到变量）
$SESSION_ID = agentarts runtime start-session -a my-agent

# 2. 上传文件
agentarts runtime upload-files -a my-agent -s $SESSION_ID -f data.csv

# 3. 执行命令
agentarts runtime exec-command -a my-agent -s $SESSION_ID `
  "python process.py data.csv"

# 4. 下载结果
agentarts runtime download-files -a my-agent -s $SESSION_ID `
  -p /home/user/result.csv -o ./result.csv

# 5. 关闭会话
agentarts runtime stop-session -a my-agent -s $SESSION_ID
```
