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
