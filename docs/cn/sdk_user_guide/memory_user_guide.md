# Memory SDK 使用指南

AgentArts Memory SDK 提供对话记忆管理能力，支持空间管理、会话管理、消息存储和记忆搜索。

> 生产建议：复用并关闭客户端；API Key 由环境或 Secret Manager 注入，不写入源码。同步与异步客户端共享相同的控制面/数据面契约。

## 概述

Memory SDK 采用**双平面架构**：

- **控制平面**（AK/SK 签名）— 管理空间（Space）和 API Key
- **数据平面**（API Key 认证）— 管理会话（Session）、消息（Message）和记忆（Memory）

## 身份验证

### 控制平面（AK/SK）

```bash
export HUAWEICLOUD_SDK_AK="your-access-key"
export HUAWEICLOUD_SDK_SK="your-secret-key"
```

```powershell
$env:HUAWEICLOUD_SDK_AK = "your-access-key"
$env:HUAWEICLOUD_SDK_SK = "your-secret-key"
```

### 数据平面（API Key）

```bash
export HUAWEICLOUD_SDK_MEMORY_API_KEY="your-memory-api-key"
```

```powershell
$env:HUAWEICLOUD_SDK_MEMORY_API_KEY = "your-memory-api-key"
```

### 数据面端点（可选）

```bash
export AGENTARTS_MEMORY_DATA_ENDPOINT="https://memory.cn-southwest-2.huaweicloud-agentarts.com"
```

```powershell
$env:AGENTARTS_MEMORY_DATA_ENDPOINT = "https://memory.cn-southwest-2.huaweicloud-agentarts.com"
```

## 快速入门

### 客户端模式

```java
import com.huaweicloud.agentarts.sdk.memory.MemoryClient;
import com.huaweicloud.agentarts.sdk.memory.model.*;
import java.util.List;

try (MemoryClient client = new MemoryClient()) {
    // 创建空间（控制平面）
    SpaceInfo space = client.createSpace("my-space", 168, "测试空间");

    // 创建会话
    SessionInfo session = client.createMemorySession(space.getId());

    // 添加消息
    TextMessage userMsg = new TextMessage("user", "你好！");
    TextMessage assistantMsg = new TextMessage("assistant", "你好！有什么可以帮助你的？");
    client.addMessages(space.getId(), session.getId(), List.of(userMsg, assistantMsg));

    // 获取最近消息
    List<MessageInfo> messages = client.getLastKMessages(session.getId(), 10, space.getId());

    // 搜索记忆
    MemorySearchResponse searchResult = client.searchMemories(space.getId());

    // 列出记忆
    MemoryListResponse memories = client.listMemories(space.getId());
}
```

### 会话模式（MemorySession）

`MemorySession` 预绑定 space_id 和 session_id，简化 API 调用：

```java
import com.huaweicloud.agentarts.sdk.memory.MemorySession;
import com.huaweicloud.agentarts.sdk.memory.model.*;
import java.util.List;

try (MemorySession session = MemorySession.of(
        "space-123", "user-456", null, "cn-southwest-2", null)) {
    // 添加消息
    session.addMessages(List.of(new TextMessage("user", "Hello!")));

    // 获取最近消息
    List<MessageInfo> last = session.getLastKMessages(10);

    // 搜索记忆
    MemorySearchResponse result = session.searchMemories();

    // 列出记忆
    MemoryListResponse memories = session.listMemories();
}
```

### 异步模式

`AsyncMemoryClient` 与 `AsyncMemorySession` 的数据面方法返回 cold Reactor `Mono`：创建对象不会发请求，订阅时才使用非阻塞 HTTP transport。不要在 event-loop 线程调用 `.block()`。

```java
import com.huaweicloud.agentarts.sdk.memory.AsyncMemorySession;
import com.huaweicloud.agentarts.sdk.memory.model.TextMessage;
import java.util.List;

try (AsyncMemorySession session = AsyncMemorySession.of("space-123", "user-456")) {
    session.initialize()
            .then(session.addMessages(List.of(new TextMessage("user", "Hello"))))
            .then(session.getLastKMessages(10))
            .doOnNext(messages -> System.out.println("count=" + messages.size()))
            .block(); // 仅用于命令行示例；服务端应返回/组合 Mono
}
```

包装外部 `MemoryClient` 构造的 session 不拥有该客户端；`of(...)` 创建的 session 会在关闭时释放内部客户端。失败的懒初始化可重试，并发订阅共享同一次初始化请求。

## API 参考

### 构造函数

```java
// 完整参数
MemoryClient client = new MemoryClient(regionName, apiKey, verifySsl);

// 默认 SSL 验证
MemoryClient client = new MemoryClient(regionName, apiKey);

// 使用环境变量默认值
MemoryClient client = new MemoryClient();
```

### 控制平面：空间管理

#### createSpace — 创建空间

```java
SpaceInfo space = client.createSpace(
    "my-space",     // name: 空间名称
    168,            // messageTtlHours: 消息 TTL（小时）
    "描述文本"       // description: 可选描述
);
```

**返回**：`SpaceInfo`（id, name, description, status, apiKey, apiKeyId, publicDomain, createdAt, updatedAt）

#### listSpaces — 列出空间

```java
SpaceListResponse result = client.listSpaces(20, 0);  // limit, offset
List<SpaceInfo> spaces = result.getItems();
int total = result.getTotal();
```

#### getSpace — 获取空间

```java
SpaceInfo space = client.getSpace("space-id-123");
```

#### updateSpace — 更新空间

```java
SpaceInfo updated = client.updateSpace(
    "space-id-123",     // spaceId
    "new-name",         // name（可选，null 跳过）
    "new description",  // description（可选）
    336                 // messageTtlHours（可选）
);
```

#### deleteSpace — 删除空间

```java
client.deleteSpace("space-id-123");
```

#### createApiKey — 创建 API Key

```java
ApiKeyInfo key = client.createApiKey();
// key.getApiKey() → API Key 字符串
// key.getId() → Key ID
```

### 数据平面：会话管理

#### createMemorySession — 创建会话

```java
SessionInfo session = client.createMemorySession(
    spaceId,        // 空间 ID
    null,           // 可选 session ID（null 自动生成）
    "actor-1",      // 可选 actor ID
    null            // 可选 assistant ID
);
```

### 数据平面：消息管理

#### addMessages — 添加消息

```java
TextMessage msg1 = new TextMessage("user", "你好！");
TextMessage msg2 = new TextMessage("assistant", "你好！");
ToolCallMessage toolCall = new ToolCallMessage("call-1", "search", "{\"query\":\"test\"}");
ToolResultMessage toolResult = new ToolResultMessage("call-1", "搜索结果");

MessageBatchResponse batch = client.addMessages(spaceId, sessionId, List.of(msg1, msg2));
// batch.getItems() → 添加的消息列表
```

#### 消息类型

```java
// 文本消息
TextMessage textMsg = new TextMessage("user", "Hello");
textMsg.setActorId("actor-1");

// 工具调用消息
ToolCallMessage toolCallMsg = new ToolCallMessage("call-id", "tool-name", "{\"arg\":\"value\"}");

// 工具结果消息
ToolResultMessage toolResultMsg = new ToolResultMessage("call-id", "result data");
```

#### listMessages — 列出消息

```java
MessageListResponse result = client.listMessages(spaceId, sessionId, 10, 0);
// result.getItems() → List<MessageInfo>
// result.getTotal() → 总数
```

#### getLastKMessages — 获取最近 K 条消息

```java
List<MessageInfo> messages = client.getLastKMessages(sessionId, 10, spaceId);
```

#### getMessage — 获取单条消息

```java
MessageInfo msg = client.getMessage("message-id", spaceId, sessionId);
```

### 数据平面：记忆管理

#### searchMemories — 搜索记忆

```java
// 无过滤条件
MemorySearchResponse result = client.searchMemories(spaceId);

// 带过滤条件
MemorySearchFilter filter = new MemorySearchFilter();
filter.setQuery("关键词");
filter.setActorId("actor-1");
filter.setTopK(5);
filter.setMinScore(0.7);
MemorySearchResponse result = client.searchMemories(spaceId, filter);
```

#### listMemories — 列出记忆

```java
MemoryListResponse result = client.listMemories(spaceId);
// 或带分页和过滤
MemoryListResponse result = client.listMemories(spaceId, 10, 0, null);
```

#### getMemory — 获取单条记忆

```java
MemoryInfo memory = client.getMemory(spaceId, "memory-id");
```

#### deleteMemory — 删除记忆

```java
client.deleteMemory(spaceId, "memory-id");
```

## MemorySession 会话包装器

```java
MemorySession session = MemorySession.of(
    "space-123",    // spaceId
    "user-456",     // actorId
    null,           // sessionId（null 自动创建）
    "cn-southwest-2",  // regionName
    null             // apiKey；null 时从环境读取
);

// 预绑定的 API 调用
session.addMessages(List.of(new TextMessage("user", "Hello")));
List<MessageInfo> last = session.getLastKMessages(10);
MessageListResponse listed = session.listMessages();
MemorySearchResponse searched = session.searchMemories();
MemoryListResponse memories = session.listMemories();
MemoryInfo memory = session.getMemory("memory-id");
session.deleteMemory("memory-id");

// 关闭时释放资源
session.close();
```

## 返回类型

### SpaceInfo

| 字段 | 类型 | 说明 |
|------|------|------|
| `getId()` | String | 空间 ID |
| `getName()` | String | 空间名称 |
| `getDescription()` | String | 描述 |
| `getStatus()` | String | 状态 |
| `getApiKey()` | Object | API Key（创建时返回明文，get 时可能为加密标记） |
| `getApiKeyId()` | String | API Key ID |
| `getPublicDomain()` | String | 公网域名 |
| `getCreatedAt()` | String | 创建时间 |
| `getUpdatedAt()` | String | 更新时间 |

### SessionInfo

| 字段 | 类型 | 说明 |
|------|------|------|
| `getId()` | String | 会话 ID |
| `getSpaceId()` | String | 空间 ID |
| `getActorId()` | String | Actor ID |
| `getAssistantId()` | String | Assistant ID |
| `getCreatedAt()` | String | 创建时间 |

### MessageInfo

| 字段 | 类型 | 说明 |
|------|------|------|
| `getId()` | String | 消息 ID |
| `getSessionId()` | String | 会话 ID |
| `getSeq()` | int | 消息序号 |
| `getRole()` | String | 角色（user/assistant/system/tool） |
| `getParts()` | List | 消息内容（OpenAPI parts 格式） |
| `getCreatedAt()` | String | 创建时间 |

### MemoryInfo

| 字段 | 类型 | 说明 |
|------|------|------|
| `getId()` | String | 记忆 ID |
| `getSpaceId()` | String | 空间 ID |
| `getContent()` | String | 记忆内容 |
| `getMemoryType()` | String | 记忆类型 |
| `getCreatedAt()` | String | 创建时间 |

## 错误处理

```java
try {
    SpaceInfo space = client.createSpace("my-space");
} catch (RuntimeException e) {
    // 记录状态/请求标识和脱敏摘要，不记录请求体或凭证
    System.err.println("Memory request failed: " + e.getMessage());
}
```

## 完整示例

参见 `agentarts-sdk-examples` 模块中的 `MemoryUsageExample`：

```bash
export HUAWEICLOUD_SDK_MEMORY_API_KEY=your-api-key
export AGENTARTS_MEMORY_SPACE_ID=your-space-id
mvn compile exec:java -pl agentarts-sdk-examples \
  -Dexec.mainClass="com.huaweicloud.agentarts.examples.memory.MemoryUsageExample"
```

```powershell
$env:HUAWEICLOUD_SDK_MEMORY_API_KEY = "your-api-key"
$env:AGENTARTS_MEMORY_SPACE_ID = "your-space-id"
mvn compile exec:java -pl agentarts-sdk-examples `
  "-Dexec.mainClass=com.huaweicloud.agentarts.examples.memory.MemoryUsageExample"
```
