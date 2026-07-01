# Basic Runtime Example

最简单的 Agent 示例，展示如何使用 AgentArts SDK 创建一个基础 Agent。

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
