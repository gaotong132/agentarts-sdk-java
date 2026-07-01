# Runtime 运行时 SDK 使用指南

AgentArts Runtime SDK 提供基于 Vert.x 的轻量级 HTTP/WebSocket/SSE 服务器，用于将 Agent 逻辑包装为标准服务端点。

## 概述

运行时 SDK 包含三个核心组件：

- **AgentArtsRuntimeApp** — HTTP 服务器，暴露 `/invocations`、`/ping`、`/ws` 端点
- **RequestContext** — 请求上下文，携带 session_id、request_id、user_id 等信息
- **AgentArtsRuntimeContext** — 线程本地上下文，支持深层调用栈中的状态传递

## 服务端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/invocations` | POST | 主入口点，处理 Agent 调用 |
| `/ping` | GET | 健康检查 |
| `/ws` | WebSocket | 双向流式通信 |

## 快速入门

### 基本用法

```java
import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;
import com.huaweicloud.agentarts.sdk.core.PingStatus;
import java.util.Map;

AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();

// 注册入口点
app.setEntrypoint((Map<String, Object> payload) -> {
    String message = (String) payload.get("message");
    return Map.of("response", "Hello: " + message);
});

// 注册健康检查
app.setPingHandler(() -> PingStatus.HEALTHY);

// 启动服务器
app.run(8080);
```

### 使用 RequestContext

```java
import com.huaweicloud.agentarts.sdk.runtime.context.RequestContext;

app.setEntrypoint((Map<String, Object> payload, RequestContext ctx) -> {
    String sessionId = ctx.getSessionId();
    String requestId = ctx.getRequestId();
    String userId = ctx.getUserId();

    return Map.of(
        "response", "Processed",
        "session_id", sessionId != null ? sessionId : "",
        "request_id", requestId
    );
});
```

### 流式响应（SSE）

返回 `Flux` 对象即可自动启用 SSE 流式传输：

```java
import reactor.core.publisher.Flux;
import java.time.Duration;

app.setEntrypoint((Map<String, Object> payload) -> {
    // 返回 Flux 自动启用 SSE 流式传输
    return Flux.interval(Duration.ofMillis(500))
        .take(5)
        .map(i -> Map.of("chunk", i, "data", "streaming item " + i));
});
```

客户端接收格式：

```
data: {"chunk":0,"data":"streaming item 0"}

data: {"chunk":1,"data":"streaming item 1"}

...
```

## 初始化参数

```java
// 自定义并发限制
AgentArtsRuntimeApp app = new AgentArtsRuntimeApp(
    50  // maxConcurrency: 最大并发请求数（默认 15）
);
```

### 启动方法

```java
// 指定端口启动
app.run(8080);

// 使用默认端口（8080）
app.run();
```

## 装饰器模式（注解）

Java SDK 提供与 Python 等价的注解：

### @Entrypoint — 主处理程序

```java
import com.huaweicloud.agentarts.sdk.core.annotation.Entrypoint;

@Entrypoint
public Map<String, Object> handle(Map<String, Object> payload) {
    return Map.of("response", "processed");
}
```

### @Ping — 健康检查

```java
import com.huaweicloud.agentarts.sdk.core.annotation.Ping;

@Ping
public PingStatus healthCheck() {
    return PingStatus.HEALTHY;
}
```

### @WebSocket — WebSocket 处理

```java
import com.huaweicloud.agentarts.sdk.core.annotation.WebSocket;

@WebSocket
public void handleWebSocket(ServerWebSocket ws, RequestContext ctx) {
    ws.textMessageHandler(text -> {
        ws.writeTextMessage("Echo: " + text);
    });
}
```

## 健康检查管理

```java
// 默认健康检查
app.setPingHandler(() -> PingStatus.HEALTHY);

// 强制设置不健康状态（用于优雅关闭）
app.forcePingStatus(PingStatus.UNHEALTHY);

// 恢复正常健康检查
app.forcePingStatus(null);
```

### PingStatus 枚举

| 值 | 说明 |
|---|---|
| `HEALTHY` | 服务健康 |
| `HEALTHY_BUSY` | 服务健康但繁忙（有异步任务运行中） |
| `UNHEALTHY` | 服务不健康 |

## 并发控制

当并发请求达到 `maxConcurrency` 限制时，新请求返回 HTTP 503：

```json
{"error": "Service busy - maximum concurrency reached", "message": ""}
```

## 异步任务跟踪

```java
// 注册后台任务
long taskId = app.addTask("background-processing");

// 任务完成后标记
app.completeTask(taskId);

// 检查是否有运行中的任务
if (app.hasRunningTasks()) {
    // 返回 HEALTHY_BUSY 状态
}
```

## RequestContext 属性

`RequestContext` 从 HTTP 请求头自动提取以下属性：

| 属性 | HTTP 请求头 | 说明 |
|------|------------|------|
| `getRequestId()` | `X-Request-Id` | 请求唯一标识 |
| `getSessionId()` | `x-hw-agentarts-session-id` | 会话 ID |
| `getUserId()` | `X-HW-AgentGateway-User-Id` | 用户 ID |
| `getWorkloadAccessToken()` | `X-HW-AgentGateway-Workload-Access-Token` | 工作负载访问令牌 |

## AgentArtsRuntimeContext（线程本地上下文）

```java
import com.huaweicloud.agentarts.sdk.runtime.context.AgentArtsRuntimeContext;

// 在深层调用栈中获取上下文
public class BusinessLogic {
    public void process() {
        String sessionId = AgentArtsRuntimeContext.getSessionId();
        String requestId = AgentArtsRuntimeContext.getRequestId();
        String userId = AgentArtsRuntimeContext.getUserId();
        // 使用上下文信息...
    }
}
```

### 完整方法列表

| 方法 | 说明 |
|------|------|
| `getSessionId()` / `setSessionId(String)` | 会话 ID |
| `getRequestId()` / `setRequestId(String)` | 请求 ID |
| `getUserId()` / `setUserId(String)` | 用户 ID |
| `getWorkloadAccessToken()` / `setWorkloadAccessToken(String)` | 工作负载访问令牌 |
| `getUserToken()` / `setUserToken(String)` | 用户令牌 |
| `getOAuth2CallbackUrl()` / `setOAuth2CallbackUrl(String)` | OAuth2 回调 URL |
| `getOAuth2CustomState()` / `setOAuth2CustomState(String)` | OAuth2 自定义状态 |
| `clear()` | 清除所有上下文变量（必须在 finally 块中调用） |

## 完整示例

参见 `agentarts-sdk-examples` 模块中的 `BasicRuntimeExample`：

```bash
mvn compile exec:java -pl agentarts-sdk-examples \
  -Dexec.mainClass="com.huaweicloud.agentarts.examples.BasicRuntimeExample"
```

## 响应格式

### 同步 JSON 响应

```
HTTP/1.1 200 OK
Content-Type: application/json
x-hw-agentarts-session-id: session-123

{"response": "Hello!", "session_id": "session-123"}
```

### SSE 流式响应

```
HTTP/1.1 200 OK
Content-Type: text/event-stream
x-hw-agentarts-session-id: session-123

data: {"chunk":0,"text":"item 0"}

data: {"chunk":1,"text":"item 1"}

```

### 错误响应

| 状态码 | 场景 | 响应体 |
|--------|------|--------|
| 400 | 无效 JSON | `{"error": "Invalid JSON payload", "message": "..."}` |
| 404 | 未注册入口点 | `{"error": "NotFound", "message": "No entrypoint handler registered"}` |
| 500 | 处理程序异常 | `{"error": "ExceptionClassName", "message": "..."}` |
| 503 | 并发限制 | `{"error": "Service busy - maximum concurrency reached", "message": ""}` |
