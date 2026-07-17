# Basic Runtime Example

最简单的 Agent 示例，展示如何使用 AgentArts SDK 创建一个基础 Agent。

示例使用 `runUntilShutdown`，收到 JVM shutdown 或线程中断时会停止 Runtime 并释放托管资源；适合作为独立进程入口的生命周期基线。

## 快速开始

```bash
mvn compile exec:java -pl agentarts-sdk-examples \
  -Dexec.mainClass="com.huaweicloud.agentarts.examples.BasicRuntimeExample"
```

## 测试

```bash
# 健康检查
curl http://localhost:8080/ping

# 同步调用
curl -X POST http://localhost:8080/invocations \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello!"}'

# 流式调用
curl -X POST http://localhost:8080/invocations \
  -H "Content-Type: application/json" \
  -d '{"stream": true, "message": "Hello!"}'
```

## 端点说明

- `POST /invocations` — 调用 Agent 入口点（支持同步 JSON 和 SSE 流式）
- `GET /ping` — 健康检查端点

## 部署

```bash
agentarts init --name basic-agent --template basic
agentarts dev
agentarts deploy --mode cloud
```

## 其他生产示例

- `AgentScopeIntegrationExample`：AgentScope Java 1.0、按请求创建 Agent、按 session 串行、超时控制、可选 Memory 状态持久化与 Runtime host。
- `memory/MemoryUsageExample`：Memory 客户端由 Runtime 托管并在停机时关闭。
- `tools/CodeInterpreterExample`：每次调用使用 `CodeSession` 保证沙箱会话释放。

所有凭证都必须通过进程环境或 Secret Manager 注入。示例不会要求把 AK/SK/API Key 写入源码或 `.agentarts_config.yaml`。
