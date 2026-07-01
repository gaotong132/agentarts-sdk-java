# agentarts runtime — 运行时管理

`runtime` 命令提供 Agent 运行时的会话管理、文件传输和命令执行能力。

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
| `--agent` | `-a` | Agent 名称 |
| `--region` | `-r` | 华为云区域 |
| `--endpoint` | | 自定义端点 URL |
| `--bearer-token` | | Bearer 认证令牌 |
| `--skip-ssl` | | 跳过 SSL 验证 |

## start-session — 启动会话

```bash
agentarts runtime start-session -a my-agent
agentarts runtime start-session -a my-agent -r cn-southwest-2
agentarts runtime start-session -a my-agent --bearer-token your-token
```

返回会话 ID，用于后续操作。

## stop-session — 关闭会话

```bash
agentarts runtime stop-session -a my-agent --session-id session-123
```

## invoke — 调用 Agent

```bash
agentarts runtime invoke -a my-agent --session-id session-123 \
  --payload '{"message":"Hello!"}'

# 自定义路径
agentarts runtime invoke -a my-agent --custom-path /custom/invoke \
  --payload '{"data":"test"}'

# 设置超时
agentarts runtime invoke -a my-agent --timeout 120 \
  --payload '{"message":"Complex task"}'
```

## exec-command — 执行命令

```bash
# 基本命令执行
agentarts runtime exec-command -a my-agent --session-id session-123 \
  --command "ls -la"

# 带超时
agentarts runtime exec-command -a my-agent --session-id session-123 \
  --command "python script.py" --timeout 60
```

## upload-files — 上传文件

```bash
# 上传文件到默认目录
agentarts runtime upload-files -a my-agent --session-id session-123 \
  --files file1.txt file2.py

# 上传到指定远程目录
agentarts runtime upload-files -a my-agent --session-id session-123 \
  --files file1.txt --remote-path /home/user/data/
```

## download-files — 下载文件

```bash
# 下载单个文件
agentarts runtime download-files -a my-agent --session-id session-123 \
  --paths /home/user/output.txt

# 下载多个文件
agentarts runtime download-files -a my-agent --session-id session-123 \
  --paths /home/user/result.csv /home/user/log.txt

# 指定输出目录
agentarts runtime download-files -a my-agent --session-id session-123 \
  --paths /home/user/output.txt --output-dir ./downloads/
```

## 完整会话生命周期

```bash
# 1. 启动会话
SESSION_ID=$(agentarts runtime start-session -a my-agent)

# 2. 上传文件
agentarts runtime upload-files -a my-agent --session-id $SESSION_ID \
  --files data.csv

# 3. 执行命令
agentarts runtime exec-command -a my-agent --session-id $SESSION_ID \
  --command "python process.py data.csv"

# 4. 下载结果
agentarts runtime download-files -a my-agent --session-id $SESSION_ID \
  --paths /home/user/result.csv

# 5. 关闭会话
agentarts runtime stop-session -a my-agent --session-id $SESSION_ID
```
