# Code Interpreter Example

展示如何使用 AgentArts Code Interpreter 在沙箱中执行代码和命令。

## 快速开始

```bash
# 设置环境变量
export HUAWEICLOUD_SDK_CODE_INTERPRETER_API_KEY="your-api-key"
export AGENTARTS_CODE_INTERPRETER_NAME="your-ci-name"

# 运行 Agent
mvn compile exec:java -pl agentarts-sdk-examples \
  -Dexec.mainClass="com.huaweicloud.agentarts.examples.tools.CodeInterpreterExample"
```

## 测试

```bash
# 执行 Python 代码
curl -X POST http://localhost:8080/invocations \
  -H "Content-Type: application/json" \
  -d '{"code": "print(1+1)", "language": "python"}'

# 执行 Shell 命令
curl -X POST http://localhost:8080/invocations \
  -H "Content-Type: application/json" \
  -d '{"command": "echo hello"}'
```

## 请求参数

| 参数 | 说明 | 必需 |
|------|------|------|
| `code` | Python 代码（与 command 二选一） | 否 |
| `language` | 编程语言（默认 python） | 否 |
| `command` | Shell 命令（与 code 二选一） | 否 |

## 环境变量

| 变量名 | 说明 | 必需 |
|--------|------|------|
| `HUAWEICLOUD_SDK_CODE_INTERPRETER_API_KEY` | 代码解释器 API Key | 是 |
| `HUAWEICLOUD_SDK_REGION` | 华为云区域 | 否（默认 cn-southwest-2） |
| `AGENTARTS_CODE_INTERPRETER_NAME` | 预创建的代码解释器名称 | 是 |
