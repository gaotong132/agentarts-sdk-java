# Runtime 运行时 SDK 使用指南

AgentArts Runtime SDK 提供基于 Vert.x 的轻量级 HTTP/WebSocket/SSE 服务器，以及管理云端 Agent 的客户端，用于将 Agent 逻辑包装为标准服务端点并管理其全生命周期。

## 概述

运行时 SDK 包含五个核心组件：

- **AgentArtsRuntimeApp** — HTTP 服务器，暴露 `/invocations`、`/ping`、`/ws` 端点
- **RequestContext** — 请求上下文，携带 session_id、request_id、user_id 等信息
- **AgentArtsRuntimeContext** — 线程本地上下文，支持深层调用栈中的状态传递
- **RuntimeClient** — 双平面客户端，管理云端 Agent 生命周期（控制面 AK/SK）+ 数据面操作（invoke/exec/upload/download/sessions）
- **LocalRuntimeClient** — 本地开发客户端，连接本地 AgentArts Runtime 服务器（无需云凭证）

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

```powershell
mvn compile exec:java -pl agentarts-sdk-examples `
  "-Dexec.mainClass=com.huaweicloud.agentarts.examples.BasicRuntimeExample"
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

## RuntimeClient — 云端 Agent 管理客户端

RuntimeClient 采用**双平面架构**：

| 平面 | 认证方式 | 职责 |
|------|----------|------|
| **控制面**（Control Plane） | AK/SK 签名 | Agent CRUD、Endpoint CRUD |
| **数据面**（Data Plane） | SDK-HMAC-SHA256 或 Bearer Token | invoke、exec、upload、download、sessions |

### 创建客户端

```java
import com.huaweicloud.agentarts.sdk.service.runtime.RuntimeClient;

// 默认（自动检测 region，SSL 验证开启）
try (RuntimeClient client = new RuntimeClient()) { ... }

// 指定 region
try (RuntimeClient client = new RuntimeClient("cn-north-4")) { ... }

// 指定 region + 关闭 SSL 验证
try (RuntimeClient client = new RuntimeClient("cn-southwest-2", false)) { ... }

// 设置 Bearer Token（数据面认证）
client.setAuthToken("your-bearer-token");
```

### 控制面：Agent CRUD

```java
// 创建 Agent（仅必填参数）
AgentInfo agent = client.createAgent("my-agent", "my agent description");
String agentId = agent.getId();

// 创建 Agent（完整参数，用 CreateAgentRequest 构造）
CreateAgentRequest req = new CreateAgentRequest()
    .withName("my-agent")
    .withDescription("description")
    .withArtifactSource(artifactSourceConfig)      // Map: 制品源配置（可省略）
    .withIdentityConfiguration(identityConfig)     // Map: 身份配置（可省略）
    .withInvokeConfig(invokeConfig)                // Map: 调用配置（可省略）
    .withNetworkConfig(networkConfig)              // Map: 网络配置（可省略）
    .withObservability(observabilityConfig)        // Map: 可观测性配置（可省略）
    .withExecutionAgencyName("agency-name")        // 执行 Agency（可省略）
    .withAgentGatewayId("gateway-id")              // Agent 网关 ID（可省略）
    .withEnvironmentVariables(envVars)             // List<Map<String,String>>: 环境变量（可省略）
    .withTags(tagsConfig);                         // List<Map<String,String>>: 标签（可省略）
agent = client.createAgent(req);

// 更新 Agent
AgentInfo updated = client.updateAgent(agentId,
    new UpdateAgentRequest().withDescription("updated description"));

// 创建或更新（先按名称查找，存在则更新，不存在则创建）
agent = client.createOrUpdateAgent("my-agent",
    new CreateAgentRequest().withDescription("description"));

// 列出 Agent（分页）
AgentListResponse resp = client.getAgents("name-filter", 1, 10);
List<AgentInfo> agents = resp.getItems();
List<AgentInfo> allAgents = client.getAgents().getItems();  // 默认分页

// 按名称查找（null 表示未找到）
AgentInfo found = client.findAgentByName("my-agent");

// 按 ID 查找
AgentInfo foundById = client.findAgentById(agentId);

// 按名称删除（内部先查找 ID，再删除）
boolean deleted = client.deleteAgentByName("my-agent");
```

### 控制面：Endpoint CRUD

```java
// 创建 Endpoint（仅 agentId + 名称，返回含 endpoint UUID）
AgentEndpointInfo ep = client.createAgentEndpoint(agentId, "my-endpoint");
String endpointId = ep.getId();  // 后续 update/find/delete 用此 UUID

// 创建 Endpoint（带类型、配置、目标版本名）
ep = client.createAgentEndpoint(
    agentId, "my-endpoint", "invocations", Map.of("timeout", 30), "v1"
);

// 更新 Endpoint（第 2 个参数是 endpoint UUID，不是名称）
AgentEndpointInfo updated = client.updateAgentEndpoint(
    agentId, endpointId, Map.of("timeout", 60)
);

// 查找 Endpoint（第 2 个参数是 endpoint UUID）
AgentEndpointInfo found = client.findAgentEndpoint(agentId, endpointId);

// 删除 Endpoint（第 2 个参数是 endpoint UUID）
client.deleteAgentEndpoint(agentId, endpointId);
```

### 数据面：调用与执行

```java
// 调用 Agent
Map<String, Object> result = client.invokeAgent("my-agent", sessionId, "{\"msg\":\"hello\"}");

// 调用 Agent（完整参数）
Map<String, Object> result = client.invokeAgent(
    "my-agent", sessionId, payload,
    bearerToken,    // Bearer Token 覆盖（可 null）
    endpoint,       // 自定义 endpoint URL（可 null）
    900,            // 超时秒数
    userId,         // 用户 ID（可 null）
    customPath      // 自定义路径，追加到 /invocations（可 null）
);

// 执行命令
Map<String, Object> cmdResult = client.execCommand("my-agent", sessionId, "echo hello");

// 执行命令（完整参数：命令列表 + chunked 传输）
Map<String, Object> cmdResult = client.execCommand(
    "my-agent", sessionId,
    List.of("ls", "-la"),  // 命令参数列表
    true,                  // chunked 传输
    null, null, null, 900
);
```

### 数据面：文件操作

```java
// 上传文件
Map<String, Object> uploadResult = client.uploadFiles("my-agent", sessionId,
    List.of(Map.of(
        "path", "/home/user/test.txt",
        "content", "hello world",
        "description", "test file"
    ))
);

// 上传文件（完整参数：指定远程路径、权限）
Map<String, Object> uploadResult = client.uploadFiles(
    "my-agent", sessionId, files,
    "/home/user/",    // 远程目录
    1000,             // file_user_id（可 null）
    1000,             // file_group_id（可 null）
    "0644",           // file_mode（可 null）
    null, null, null, 900
);

// 下载文件
RequestResult downloadResult = client.downloadFiles("my-agent", sessionId, "/home/user/test.txt");
if (downloadResult.isSuccess()) {
    // 处理下载结果
}

// 下载文件（完整参数：递归下载）
RequestResult downloadResult = client.downloadFiles(
    "my-agent", sessionId, "/home/user/dir",
    true,              // 递归下载
    null, null, null, 900
);
```

### 数据面：会话管理

```java
// 启动会话
Map<String, Object> session = client.startSession("my-agent");
String sessionId = (String) session.get("session_id");

// 执行操作...
client.execCommand("my-agent", sessionId, "echo hello");
client.uploadFiles("my-agent", sessionId, files);

// 停止会话
client.stopSession("my-agent", sessionId);
```

### RuntimeClient 完整方法列表

| 平面 | 方法 | 说明 |
|------|------|------|
| 控制面 | `createAgent(name, description)` | 创建 Agent（返回 `AgentInfo`） |
| 控制面 | `createAgent(CreateAgentRequest req)` | 创建 Agent（完整参数，用 builder 构造） |
| 控制面 | `updateAgent(id, UpdateAgentRequest req)` | 更新 Agent（返回 `AgentInfo`） |
| 控制面 | `createOrUpdateAgent(name, CreateAgentRequest req)` | 创建或更新 Agent |
| 控制面 | `getAgents(name, offset, limit)` | 列出 Agent（返回 `AgentListResponse`，`.getItems()` 取列表） |
| 控制面 | `getAgents()` | 列出 Agent（默认分页） |
| 控制面 | `findAgentByName(name)` | 按名称查找（返回 `AgentInfo`，未找到 null） |
| 控制面 | `findAgentById(id)` | 按 ID 查找（返回 `AgentInfo`） |
| 控制面 | `deleteAgentByName(name)` | 按名称删除 |
| 控制面 | `createAgentEndpoint(agentId, name)` | 创建 Endpoint（返回 `AgentEndpointInfo`，含 UUID） |
| 控制面 | `createAgentEndpoint(agentId, name, type, config, targetVersionName)` | 创建 Endpoint（完整参数） |
| 控制面 | `updateAgentEndpoint(agentId, endpointId, config)` | 更新 Endpoint（第 2 参为 endpoint UUID） |
| 控制面 | `deleteAgentEndpoint(agentId, endpointId)` | 删除 Endpoint（第 2 参为 endpoint UUID） |
| 控制面 | `findAgentEndpoint(agentId, endpointId)` | 查找 Endpoint（第 2 参为 endpoint UUID） |
| 数据面 | `invokeAgent(name, sessionId, payload)` | 调用 Agent |
| 数据面 | `invokeAgent(name, sid, payload, token, endpoint, timeout, userId, customPath)` | 调用 Agent（完整参数） |
| 数据面 | `execCommand(name, sessionId, command)` | 执行命令 |
| 数据面 | `execCommand(name, sid, cmdList, chunked, token, endpoint, userId, timeout)` | 执行命令（完整参数） |
| 数据面 | `uploadFiles(name, sessionId, files)` | 上传文件 |
| 数据面 | `uploadFiles(name, sid, files, path, uid, gid, mode, token, ep, userId, timeout)` | 上传文件（完整参数） |
| 数据面 | `downloadFiles(name, sessionId, path)` | 下载文件 |
| 数据面 | `downloadFiles(name, sid, path, recursive, token, ep, userId, timeout)` | 下载文件（完整参数） |
| 数据面 | `startSession(name)` | 启动会话 |
| 数据面 | `startSession(name, token, endpoint, userId, timeout)` | 启动会话（完整参数） |
| 数据面 | `stopSession(name, sessionId)` | 停止会话 |
| 数据面 | `stopSession(name, sid, token, endpoint, userId, timeout)` | 停止会话（完整参数） |

## LocalRuntimeClient — 本地开发客户端

用于连接本地运行的 AgentArts Runtime 服务器，无需云凭证，适合开发调试。

```java
import com.huaweicloud.agentarts.sdk.service.runtime.LocalRuntimeClient;

// 默认连接 http://localhost:8080
try (LocalRuntimeClient client = new LocalRuntimeClient()) {
    // 调用本地 Agent
    Map<String, Object> result = client.invokeAgent("{\"message\":\"hello\"}");

    // 健康检查
    Map<String, Object> ping = client.pingAgent();
}

// 自定义端口和主机
try (LocalRuntimeClient client = new LocalRuntimeClient(3000, "127.0.0.1", 60)) {
    // 带 session ID 和 Bearer Token 调用
    Map<String, Object> result = client.invokeAgent(
        "{\"message\":\"hello\"}",
        "session-123",     // sessionId（可 null）
        "bearer-token",    // bearerToken（可 null）
        "user-456",        // userId（可 null）
        "custom-path"      // customPath（可 null）
    );

    // 带 session ID 健康检查
    Map<String, Object> ping = client.pingAgent("session-123");
}
```

### LocalRuntimeClient 方法列表

| 方法 | 说明 |
|------|------|
| `invokeAgent(payload)` | 调用本地 Agent |
| `invokeAgent(payload, sessionId, bearerToken, userId, customPath)` | 调用本地 Agent（完整参数） |
| `pingAgent()` | 健康检查 |
| `pingAgent(sessionId)` | 带 session ID 的健康检查 |
| `getPort()` | 获取端口 |
| `getHost()` | 获取主机 |
