# 华为云 AgentArts Java SDK 实施计划

## Context

Python 版 AgentArts SDK（`D:\project\agentarts-sdk-python`）已成熟，提供 runtime（把 agent 包成 `/invocations` `/ping` `/ws` 服务）、identity 鉴权、memory、code interpreter、mcp gateway、service 层、toolkit CLI 与脚手架模板。本计划要做一份**全量 1:1 的 Java 移植版**，并要求**首版能集成到本地已 clone 的 agentscope-java**（`D:\project\agentscope-java`），同时复用本地已 clone 的华为云 Java SDK v3（`D:\project\huaweicloud-sdk-java-v3`，版本 3.1.202）。

技术选型已与用户确认：

- **运行时框架：Vert.x + Vert.x Web**（轻量，HTTP/WebSocket/SSE 齐全；且基于 Reactor，与 agentscope 的 Mono/Flux 天然对接）。
- **构建：Maven 多模块**（与 agentscope-java 一致，便于集成与发布）。
- **JDK 17+**（agentscope-java 要求 17+）。
- **复用华为云 Java SDK v3**：`agentidentity`/`iam`/`swr` 模块 API 与 Python 版 100% 对应，同步 + AsyncClient 齐全，内置 SDK-HMAC-SHA256 签名。
- **集成目标**：agentscope-java（基于 Reactor 的 agent 框架，库模式无自带 HTTP server）。

建议仓库位置：`D:\project\agentarts-sdk-java`（与另两个本地仓库同级，独立 Git 仓库）。最终目录以用户确认为准。

## 关键架构纠正（相对初版方案的修正）

1. **V11 签名归属**：V11-HMAC-SHA256 **只**用于 AgentArts 自研端点（control plane `agentarts.{region}.myhuaweicloud.com`、data plane `memory.{region}...`、runtime invoke、code interpreter、mcp gateway）的 HTTP 调用，由自研 `BaseHttpClient` 持有独立工具类 `V11Signer` 调用——**不**实现为华为云 SDK 的 `IAKSKSigner` 注入到 service client。华为云标准服务（agentidentity/iam/swr）走 SDK 内置 SDK-HMAC-SHA256，由 SDK 自行签名。依据：Python `http_client.py` 的 `SignMode` 是 `BaseHTTPClient` 自选，`signer_v11.py` 是独立类；而 `IdentityClient` 内部用 `huaweicloudsdkagentidentity` 原生 client（SDK 内置签名），不经 `BaseHTTPClient`。这把"V11 注入 SDK 是否可行"的风险直接消除。

2. **上下文类命名**：AgentArts 的上下文持有类命名 `AgentArtsRuntimeContext`（区别于 agentscope 的 `io.agentscope.core.agent.RuntimeContext`），避免集成层同名冲突。请求快照类叫 `RequestContext`（对齐 Python）。

3. **华为云模型 provider 次优先**：agentscope 已自带 dashscope/openai/anthropic 等 `ModelProvider`。首版集成的核心是 **Runtime host + MemoryAgentStateStore + AgentTool 扩展**；`HuaweiCloudModelProvider`（接 ModelArts）作为 P4 可选，不在首版强求。

## Maven 模块划分

groupId `com.huaweicloud.agentarts`，artifactId `agentarts-sdk-{module}`，版本 `${revision}`（父 POM + flatten-maven-plugin 管理发布版本）。

```
agentarts-sdk-java/
├── pom.xml                              # 父 POM
├── agentarts-sdk-bom/                   # 依赖版本 BOM
├── agentarts-sdk-core/                  # 常量/签名/HttpClient/注解/异常/配置
├── agentarts-sdk-runtime/               # AgentArtsRuntimeApp(Vert.x) + 上下文 + 路由处理器 + SSE/WS
├── agentarts-sdk-identity/              # IdentityClient + 鉴权注解实现 + TokenPoller + .agent_identity.json
├── agentarts-sdk-memory/                # MemoryClient/Async + session + inner(controlplane/dataplane)
├── agentarts-sdk-tools/                 # CodeInterpreterClient + codeSession
├── agentarts-sdk-mcpgateway/            # MCPGatewayClient + Gateway/Target/ToolCall 模型
├── agentarts-sdk-service/               # 包装华为云 SDK: IdentityServiceClient/IAMServiceClient/SWRServiceClient/RuntimeServiceClient/MemoryServiceClient/ToolsHttpClient + BaseHttpClient
├── agentarts-sdk-integration-agentscope/# agentscope 集成(桥接层 + MemoryAgentStateStore + AgentTool 扩展 + 转换器)
├── agentarts-toolkit-cli/               # Picocli 命令树 + operations + 模板 + 配置 + Docker/SWR 工具
├── agentarts-spring-boot-starter/       # (P4 可选) AutoConfiguration + Properties + Actuator
├── agentarts-sdk-all/                   # 聚合包
├── agentarts-sdk-examples/              # basic-runtime / agentscope-integration / memory / tools / cli
└── agentarts-sdk-tests/                 # 跨模块集成与 e2e
```

依赖方向：`core` ← `{runtime,identity,memory,tools,mcpgateway,service}` ← `integration-agentscope` ← `{toolkit-cli, spring-boot-starter}` ← `{all, examples, tests}`。`integration-agentscope` 依赖 `agentscope-core`（provided/optional，避免强绑定）。

## 关键技术映射

| Python                                                       | Java                                                         |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| Starlette ASGI app                                           | Vert.x `HttpServer` + `Router`（`/invocations` POST、`/ping` GET、`/ws` WebSocket） |
| `@app.entrypoint`/`@ping`/`@websocket`/`@async_task`         | 注解 `@Entrypoint`/`@Ping`/`@WebSocket`/`@AsyncTask` + 注册式扫描（非 Spring AOP，保持轻量） |
| `StreamingResponse` SSE `data:{json}\n\n`                    | Vert.x `HttpServerResponse.setChunked(true)` + `Content-Type: text/event-stream`，从 `Flux` 订阅逐块写 |
| `WebSocketRoute`                                             | Vert.x `ServerWebSocket`（`textMessageHandler`/`closeHandler`） |
| `async def`→`Mono<T>`，`AsyncGenerator`→`Flux<T>`，`await`→`flatMap`，`asyncio.run`→`block()` | Reactor（与 agentscope 对齐）                                |
| `contextvars.ContextVar`                                     | **双层**：Reactor `Context`（流式链路优先）+ `ThreadLocal<ContextSnapshot>`（同步/拦截器 fallback） |
| `run_async_in_sync_context`                                  | `AsyncExecutor.runMonoInSyncContext`：若在 Vert.x EventLoop 则切到 worker 线程 `block()`，否则直接 `block()` |
| `V11Signer`（独立类）                                        | `V11Signer` 独立工具类，供 `BaseHttpClient` 在 `SignMode.V11_HMAC_SHA256` 时调用 |
| `SDKSigner`（包装 huaweicloudsdkcore）                       | 直接复用华为云 Java SDK 内置 `AKSKSigner`（`HttpConfig.withSigningAlgorithm(HMAC_SHA256)`） |
| `BaseHTTPClient`（requests）                                 | `BaseHttpClient` 基于 Vert.x `WebClient`，支持 `SignMode`、流式响应检测（`text/event-stream`/`application/x-ndjson`）、`setAuthToken`/`clearAuth` |
| `tenacity` 重试（429/≥500，EQUAL_JITTER，3 次）              | Reactor `Retry.backoff` + jitter，过滤 `ServerException` 429/≥500；华为云 SDK 自带 Invoker 重试用于标准服务 |
| `.agentarts_config.yaml`（Pydantic）                         | Jackson `YAMLFactory` + `AgentArtsConfig` POJO（字段对齐 Python 版：base/swr_config/runtime.invoke_config/network_config/identity_configuration/observability/environment_variables/tags） |
| `constant.py` 环境变量 + endpoint 构造                       | `Constants` 类（同名环境变量 + `getRegion()` 优先级 + endpoint 构造函数） |
| `@require_access_token` 等装饰器                             | 注解 `@RequireAccessToken`/`@RequireApiKey`/`@RequireStsToken` + 拦截器在调用前获取 token 注入参数（`into` 字段名） |

## V11 签名移植要点（独立工具类）

逐行对照 `src/agentarts/sdk/utils/signer_v11.py`：timestamp `yyyyMMdd'T'HHmmss'Z'` 入 `x-sdk-date`；`signed_headers` 小写排序；`CanonicalRequest` = method + canonical_uri（分段 `URLEncoder` safe `~`，末尾强制 `/`） + canonical_query_string（key 排序，list 值排序） + canonical_headers（`k:trim\n`） + `;`.join(signed_headers) + `UNSIGNED-PAYLOAD`；`StringToSign` = `V11-HMAC-SHA256\n{ts}\n{credentialScope}\n{sha256(canonicalRequest)}`；`credentialScope` = `{date8}/{region}/apic`；签名密钥 HKDF：`PRK=HMAC-SHA256(ak作salt, sk作ikm)`，`OKM` 迭代 `HMAC-SHA256(PRK, t+info+i)` 取 32 字节 hex；`signature=HMAC-SHA256(realUseSecret, stringToSign)`；`Authorization: V11-HMAC-SHA256 Credential={ak}/{credentialScope}, SignedHeaders=..., Signature=...`。含 `X-Security-Token`（若 STS）。单测逐函数覆盖。

## agentscope-java 集成方案（首版核心）

集成形态：**AgentArts Runtime(Vert.x) host agentscope `ReActAgent` + 提供华为云能力扩展**。`integration-agentscope` 模块实现：

1. **`AgentscopeRuntimeHost`**（桥接层）：`@Entrypoint` 方法内把 AgentArts `RequestContext`（sessionId/userId）映射为 agentscope `RuntimeContext.builder().userId().sessionId().build()`，payload→`Msg`，调用 `agent.call(msg, ctx)` 返回 `Mono<Msg>` 再转 AgentArts response；流式走 `agent.streamEvents()` → `Flux` → SSE。Reactor `Context` 透传 sessionId/userId/requestId/workloadAccessToken。
2. **`MemoryAgentStateStore`** implements agentscope `AgentStateStore`：`load`/`save`/`delete` 用 AgentArts `MemoryClient` 作底层存储（替代 Python langgraph `saver`/`store`）。实现前核对 `agentscope-core/.../state/AgentStateStore.java` 真实接口签名。
3. **AgentTool 扩展**：`MCPGatewayTool`、`CodeInterpreterTool` implements agentscope `AgentTool`（`getName`/`getDescription`/`getParameters`/`callAsync(ToolCallParam)->Mono<ToolResultBlock>`），把 AgentArts 能力暴露给 agentscope `Toolkit.registerTool`。
4. **`MessageConverter`/`AgentStateConverter`**：AgentArts Message ↔ agentscope `Msg`/`ContentBlock`（Text/ToolUse/ToolResult）双向转换（替代 Python langgraph `converter`）。
5. （P4 可选）`HuaweiCloudModelProvider` + `HuaweiCloudModel` implements agentscope `Model`/`ModelProvider` SPI，接 ModelArts。

## CLI（toolkit）Java 化

- **Picocli 命令树**：`init`/`dev`/`config`(alias `configure`)/`deploy`(alias `launch`)/`invoke`/`destroy`/`runtime`(子: invoke,exec-command,upload-files,download-files,start-session,stop-session)/`mcp-gateway`/`memory`。命令顺序用 Picocli `@Command(subcommands=...)` + 自定义 listing 控制。
- **cli/operations 分层保留**：cli 层只做参数解析+输出（Picocli `@Option`），operations 层做实现。
- **模板系统**：FreeMarker（或字符串占位符，对齐 Python `manager.py` 的简单替换）。模板：`basic`（Java handler）、`agentscope`（ReActAgent + MemoryAgentStateStore）、`docker`（Dockerfile，Java base image 如 `eclipse-temurin:17-jre`）。生成 `pom.xml` + `src/main/java/.../Agent.java` + `.agentarts_config.yaml` + `Dockerfile`。
- **`.agentarts_config.yaml` 复用** Python 版格式，`base.entrypoint` 改为 Java 形态（如 `com.example.MyAgent` 或 `module:app`，由 dev 命令解析）。
- **deploy 流程**：`docker-java`（构建/tag/push/login）+ 华为云 SDK `SwrClient`（createNamespace/createRepo/createAuthorizationToken）+ `RuntimeServiceClient`（调 control plane `POST /v1/runtimes`，V11 签名，传 artifact_source/invoke_config/network_config/identity_config/observability/env_vars/tags）。对照 `operations/runtime/deploy.py` 逐段实现。
- **invoke**：local 走本地 `WebClient` POST `/invocations`；cloud 解析 agent→`_get_data_endpoint`→选 `SignMode`（IAM 用 V11_HMAC_SHA256；自定义+ bearer_token 用 SDK_HMAC_SHA256）→`RuntimeServiceClient.invokeAgent`。
- **dev**：直接在进程内起 `AgentArtsRuntimeApp`（Vert.x），端口 8080，加载 `.agentarts_config.yaml` 注入环境变量。

## 分期路线

- **P0 核心 runtime + identity + core**：`core`（Constants/V11Signer/BaseHttpClient/注解/异常）+ `runtime`（AgentArtsRuntimeApp + 上下文 + 三路由 + SSE/WS + 并发 Semaphore）+ `identity`（IdentityClient 包装华为云 `AgentIdentityClient` + 鉴权注解拦截器 + TokenPoller）+ `service` 的 IdentityServiceClient。验收：本地 `dev` 跑通 `curl POST /invocations`（同步/异步/SSE 三种返回）+ `GET /ping` 返 `Healthy`；V11 签名单测通过并能调通 AgentArts control plane。
- **P1 memory + tools + mcpgateway**：三个客户端 + inner 分层 + 数据模型 + `service` 的 Memory/Tools/IAM 客户端。验收：对照 Python 版 memory/tools/mcp 测试用例移植，行为一致。
- **P2 CLI + 模板 + deploy**：Picocli 全命令 + 模板 + 配置 + docker-java/SWR deploy 全流程。验收：`agentarts init` 生成 Java 项目、`dev` 本地起、`deploy` 完整构建→SWR→Runtime、`invoke` 调云端、`destroy` 回滚。
- **P3 agentscope 集成**：`integration-agentscope` 全部 + 示例 + 集成测试。验收：agentscope `ReActAgent` 经 AgentArts Runtime host，`/invocations` 流式 SSE 来自 `streamEvents`；`MemoryAgentStateStore` 持久化正确；`MCPGatewayTool`/`CodeInterpreterTool` 可被 agentscope `Toolkit` 调用。
- **P4 收尾 + 可选 Starter**：`spring-boot-starter`（AutoConfiguration + Properties + HealthIndicator）+ `all` 聚合 + 文档 + e2e。Starter 与 `HuaweiCloudModelProvider` 为可选。

## Goal 命令（自主执行）

每个阶段使用 `/goal` 驱动自主实现，评估器自动判断完成条件是否满足。

### P0a — 核心骨架 + V11 签名（已完成 ✅）

```
/goal agentarts-sdk-core 模块 mvn compile exit 0，V11Signer 的全部单测通过（mvn test -pl agentarts-sdk-core），
且 V11Signer 的中间值（canonicalRequest、stringToSign、realUseSecret、signature）与 Python signer_v11.py
在相同输入下的输出一致，或 20 轮后停止
```

**状态**：已提交 `2a757b9`，46 个单测全通过，18 个源文件编译成功。

### P0b — Service 层 + BaseHttpClient

```
/goal agentarts-sdk-service 模块 mvn compile exit 0，
BaseHttpClient 支持 SignMode.V11_HMAC_SHA256 和 SDK_HMAC_SHA256 两种签名模式，
IdentityServiceClient 包装华为云 AgentIdentityClient 的全部 31 个 API 方法，
SWRServiceClient 包装 SwrClient 的 createNamespace/createRepo/createAuthorizationToken，
所有 RequestResult 支持 streaming 检测（text/event-stream），或 20 轮后停止
```

### P0c — Runtime + Identity

```
/goal agentarts-sdk-runtime 和 agentarts-sdk-identity 模块 mvn compile exit 0，
Vert.x HttpServer 能在 8080 端口启动并响应 GET /ping 返回 {"status":"Healthy"}，
POST /invocations 同步模式返回 200 + JSON，SSE 模式返回 text/event-stream，
WebSocket /ws 可连接，并发 Semaphore 超限时返回 503，
IdentityClient 的鉴权注解拦截器通过动态代理实现，或 20 轮后停止
```

### P1 — Memory + Tools + MCPGateway

```
/goal agentarts-sdk-memory、agentarts-sdk-tools、agentarts-sdk-mcpgateway 三个模块 mvn compile exit 0，
所有单测 mvn test exit 0，
MemoryClient 的 API 方法签名与 Python 版 MemoryClient 一一对应（create_space/get_space/list_spaces/create_session/add_messages/get_last_k_messages/search_memories），
CodeInterpreterClient 方法与 Python CodeInterpreter 一一对应（create/start_session/invoke/execute_code/upload_file/download_file），
MCPGatewayClient 方法与 Python MCPGatewayClient 一一对应（create_mcp_gateway/create_mcp_gateway_target），或 20 轮后停止
```

### P2 — CLI + 模板 + Deploy

```
/goal agentarts-toolkit-cli 模块 mvn compile exit 0，
agentarts init -n demo -t basic 能生成项目骨架（pom.xml + src + .agentarts_config.yaml + Dockerfile），
agentarts dev 能启动本地 Vert.x 服务在 8080 端口，
agentarts invoke --local 能调通本地 /invocations，
deploy 命令能执行 docker build + SWR push + runtime create 全流程（dry-run 模式验证），或 20 轮后停止
```

### P3 — agentscope 集成

```
/goal agentarts-sdk-integration-agentscope 模块 mvn compile exit 0，
MemoryAgentStateStore 实现 agentscope AgentStateStore 接口的全部 8 个方法（save/get/getList/exists/delete/listSessionIds/close），
MCPGatewayTool 和 CodeInterpreterTool 实现 AgentTool 接口（getName/getDescription/getParameters/callAsync），
AgentscopeRuntimeHost 桥接层把 RequestContext 映射为 RuntimeContext，
ReActAgent 经 Runtime host 后 /invocations SSE 流式返回 Flux<AgentEvent>（31 种事件类型），或 20 轮后停止
```

### P4 — 收尾

```
/goal agentarts-spring-boot-starter 和 agentarts-sdk-all 模块 mvn compile exit 0，
agentarts-sdk-examples 包含 basic-runtime 和 agentscope-integration 两个示例且可运行，
agentarts-sdk-tests 跨模块集成测试 mvn test exit 0，或 15 轮后停止
```

## 提交策略（小步提交）

每个阶段内部按功能粒度拆分提交，确保每次提交可编译、可测试、可回溯：

### 提交命名规范
```
feat(scope): description     — 新功能
fix(scope): description      — 修复
test(scope): description     — 测试
refactor(scope): description — 重构
```

scope 为模块名：`core`、`service`、`runtime`、`identity`、`memory`、`tools`、`mcpgateway`、`cli`、`integration`

### P0b 提交拆分
1. `feat(service): add BaseHttpClient with Vert.x WebClient and SignMode dispatch`
2. `feat(service): add RequestResult with streaming detection (text/event-stream, application/x-ndjson)`
3. `feat(service): add IdentityServiceClient wrapping Huawei Cloud AgentIdentityClient (31 APIs)`
4. `feat(service): add IAMServiceClient and SWRServiceClient wrappers`
5. `test(service): add BaseHttpClient and service client unit tests`

### P0c 提交拆分
1. `feat(runtime): add AgentArtsRuntimeApp with Vert.x HttpServer and Router`
2. `feat(runtime): add RequestContext and AgentArtsRuntimeContext (dual-layer: Reactor Context + ThreadLocal)`
3. `feat(runtime): implement /invocations POST handler with sync/async/SSE response dispatch`
4. `feat(runtime): implement /ping GET handler with PingStatus and concurrency Semaphore`
5. `feat(runtime): implement /ws WebSocket handler`
6. `feat(identity): add IdentityClient wrapping IdentityServiceClient`
7. `feat(identity): add auth annotation interceptor via dynamic proxy (@RequireAccessToken/@RequireApiKey/@RequireStsToken)`
8. `feat(identity): add TokenPoller and .agent_identity.json config management`
9. `test(runtime): add Vert.x VertxTestContext tests for HTTP/WS/SSE endpoints`
10. `test(identity): add IdentityClient and auth interceptor unit tests`

### P1 提交拆分
1. `feat(memory): add MemoryClient with dual-plane architecture (ControlPlane + DataPlane)`
2. `feat(memory): add MemorySession convenience wrapper and data models`
3. `feat(tools): add CodeInterpreterClient with control/data plane and CodeSession`
4. `feat(mcpgateway): add MCPGatewayClient with Gateway/Target/ToolCall models`
5. `test(memory): port Python memory tests to JUnit — verify API parity`
6. `test(tools): port Python tools tests to JUnit — verify API parity`
7. `test(mcpgateway): port Python mcpgateway tests to JUnit — verify API parity`

### P2 提交拆分
1. `feat(cli): add Picocli command tree skeleton (init/dev/config/deploy/invoke/destroy/runtime/mcp-gateway/memory)`
2. `feat(cli): implement init command with FreeMarker templates (basic/agentscope/docker)`
3. `feat(cli): implement dev command — in-process Vert.x startup with config injection`
4. `feat(cli): implement config command (list/set/get/remove/set-env/remove-env/list-env/generate-dockerfile)`
5. `feat(cli): implement invoke command (local via WebClient, cloud via RuntimeServiceClient with SignMode)`
6. `feat(cli): implement deploy command (docker-java build/tag/push + SWR createNamespace/createRepo + runtime create)`
7. `feat(cli): implement destroy and runtime subcommands`
8. `test(cli): add CLI command unit tests with mock operations`

### P3 提交拆分
1. `feat(integration): add AgentscopeRuntimeHost bridge (RequestContext → RuntimeContext, payload → Msg)`
2. `feat(integration): add MemoryAgentStateStore implementing AgentStateStore (8 methods)`
3. `feat(integration): add MCPGatewayTool and CodeInterpreterTool implementing AgentTool`
4. `feat(integration): add MessageConverter (AgentArts Message ↔ agentscope Msg/ContentBlock)`
5. `feat(integration): add AgentStateConverter and SSE event bridge (31 AgentEvent types → data:json\n\n)`
6. `test(integration): add integration tests — ReActAgent via Runtime host with SSE streaming`

## Python 一致性要求

Java SDK 必须与 Python SDK 在以下维度保持完全一致：

### 1. API 方法一一对应
每个 Python Client 类的每个 public 方法，在 Java 对应类中必须有同名方法（驼峰命名转换）。参数和返回值语义一致。

| Python 模块 | Python 类 | Java 类 | 验证方式 |
|---|---|---|---|
| `sdk/service/http_client.py` | `BaseHTTPClient` | `BaseHttpClient` | SignMode 枚举值、RequestResult 字段、streaming 检测逻辑 |
| `sdk/service/identity/identity_client.py` | `IdentityClient` | `IdentityClient` | 31 个 API 方法签名一一对应 |
| `sdk/memory/` | `MemoryClient` | `MemoryClient` | control plane + data plane 方法一一对应 |
| `sdk/tools/` | `CodeInterpreter` | `CodeInterpreterClient` | control + data 方法一一对应 |
| `sdk/mcpgateway/` | `MCPGatewayClient` | `MCPGatewayClient` | gateway + target CRUD 方法一一对应 |
| `sdk/runtime/app.py` | `AgentArtsRuntimeApp` | `AgentArtsRuntimeApp` | 三路由 + SSE 格式 + 并发控制 |
| `sdk/identity/auth.py` | `require_*` 装饰器 | `@Require*` 注解 | 参数名和默认值一一对应 |
| `sdk/utils/signer_v11.py` | `V11Signer` | `V11Signer` | 中间值交叉验证（见下） |

### 2. V11 签名中间值交叉验证
使用相同输入（AK/SK/region/method/path/query/headers），Java 和 Python 必须产生：
- 相同的 `canonicalRequest` 字符串
- 相同的 `stringToSign` 字符串
- 相同的 `realUseSecret`（HKDF 输出 hex）
- 相同的 `signature`（最终签名 hex）
- 相同的 `Authorization` header 值

验证方法：在 Python 端运行 `signer_v11.py` 加入 print 中间值，在 Java 端写对应单测断言。

### 3. HTTP 契约一致
- **端点路径**：`/invocations`、`/ping`、`/ws` 完全一致
- **状态码**：200（成功）、400（JSON 解析错误）、500（内部错误）、503（并发超限）
- **SSE 格式**：`data: {json}\n\n`，无 event/id 字段
- **Header 常量**：`x-hw-agentarts-session-id`、`X-HW-AgentGateway-Workload-Access-Token`、`X-HW-AgentGateway-User-Id`
- **PingStatus 值**：`Healthy`、`HealthyBusy`、`Unhealthy`
- **错误响应格式**：`{"error": "ClassName", "message": "detail"}`

### 4. 配置文件格式一致
- `.agentarts_config.yaml` 的 YAML 结构、字段名、默认值完全一致
- `.agent_identity.json` 的 JSON 结构完全一致
- `base.language` 改为 `java17`、`base.base_image` 改为 `eclipse-temurin:17-jre`，其余字段一致

### 5. 测试用例移植
把 Python `tests/unit/sdk/` 下的每个测试文件移植为 JUnit 等价：
- 相同的测试名（驼峰转换）
- 相同的 mock 数据和断言
- 相同的边界条件和错误场景
- 每个 Python test class → 一个 Java test class，方法 1:1 映射

## 验证

- **单测**：JUnit5 + Reactor `StepVerifier`（验证 Mono/Flux）+ Vert.x `VertxTestContext`（验证 HTTP/WS）。V11 签名逐函数单测，并对照 Python 版 `signer_v11.py` 的中间值做交叉验证。
- **对照 Python 测试**：把 `tests/unit/sdk/` 下 runtime/identity/memory/tools/mcpgateway/service 的用例移植为 JUnit 等价，确保契约一致（端点、状态码、SSE 格式、header 名、PingStatus 值）。
- **端到端**：`agentarts init -n demo -t basic` → 写 `@Entrypoint` handler → `agentarts dev` → `curl POST /invocations`（含同步/异步/SSE/WS 四种）+ `GET /ping`。集成 agentscope 后用 `-t agentscope` 模板跑 `ReActAgent` 流式。
- **签名与云端联调**：用真实 AK/SK 跑一次 `agentarts invoke`（cloud 模式）验证 V11 签名与 Runtime data plane 对通（与 Python 版同账号同 region 对照）。

## 风险与权衡

- **Vert.x ↔ Reactor 桥接**：Vert.x 4 `Future` 经 `.toCompletionStage()`→`Mono.fromFuture()`/`Flux.fromStream` 适配；EventLoop 内禁止 `block()`，由 `AsyncExecutor` 切 worker 线程。需锁定 Reactor 版本与 agentscope 一致（查 `agentscope-dependencies-bom`）。
- **agentscope 接口签名漂移**：`AgentStateStore`/`Model`/`AgentTool`/`RuntimeContext` 处于 2.0.0-SNAPSHOT，实现前必须核对真实签名，不照搬本计划的草案签名。
- **V11 签名正确性**：最高风险点，逐行移植 + 中间值交叉验证 + 云端联调三重保障。
- **华为云 SDK 重试 ≠ tenacity**：标准服务用 SDK Invoker 重试；自研 `BaseHttpClient` 用 Reactor `Retry.backoff` 复刻 `EQUAL_JITTER`/3 次/429+≥500 过滤。
- **docker-java 与 Python docker SDK 差异**：逐操作对照（build/tag/push/login），SWR 登录凭证解析格式对齐 Python `operations/runtime/deploy.py`。
- **命名冲突**：AgentArts 上下文类定名 `AgentArtsRuntimeContext`，不与 agentscope `RuntimeContext` 同名。

## 实施首批要读/参考的关键文件

- `D:\project\agentarts-sdk-python\src\agentarts\sdk\runtime\app.py`（runtime 主参考）
- `D:\project\agentarts-sdk-python\src\agentarts\sdk\utils\signer_v11.py`（V11 逐行移植源）
- `D:\project\agentarts-sdk-python\src\agentarts\sdk\identity\auth.py` + `service\identity\identity_client.py`（identity 主参考）
- `D:\project\agentarts-sdk-python\src\agentarts\sdk\service\http_client.py`（BaseHttpClient 主参考）
- `D:\project\agentscope-java\agentscope-core\src\main\java\io\agentscope\core\state\AgentStateStore.java` + `...\model\Model.java` + `...\tool\AgentTool.java` + `...\agent\RuntimeContext.java`（集成层真实接口，实现前核对）
- `D:\project\huaweicloud-sdk-java-v3\services\agentidentity\src\main\java\com\huaweicloud\sdk\agentidentity\v1\AgentIdentityClient.java`（identity 复用源）
- `D:\project\agentarts-sdk-python\src\agentarts\toolkit\operations\runtime\deploy.py`（deploy 流程参考）
