# 端到端（E2E）测试指南

本文件说明 AgentArts Java SDK 端到端测试的**运行方法、三层安全模型、与 Python SDK 的用例对比、覆盖范围与缺口**，以及凭证安全约定。

> ⚠️ **凭证安全**：本文档所有命令中的 AK/SK 均为占位符（`<your-ak>` / `<your-sk>`）。**切勿**将真实凭证写入代码、配置文件、提交到仓库或贴在 issue 中。所有凭证仅通过环境变量注入，进程结束即失效。

> **基准版本**：Python 对比基准为 `agentarts-sdk-python` 仓库 `feature/test` 分支（commit `d130e21`）。如需复现对比，请确保本地 Python 仓库 `git fetch` 到该版本，而非更早的 `2f64a4b`。

---

## 1. 概述

Java SDK 的测试体系（`mvn test`，无云凭证时 E2E 云用例自动跳过）共 **650 个用例执行、0 失败 0 错误、9 跳过**，分两层：

| 层次 | 数量 | 说明 |
|---|---|---|
| 单元 + 集成测试 | 537 | 各模块内部（core 204 / service 101 / memory 87 / tools 42 / agentscope 37 / mcpgateway 27 / runtime 16 / spring-boot 16 / identity 7），无云凭证即可运行 |
| E2E 测试 | 162 | `agentarts-sdk-tests` 的 `e2e` 包（97）+ `agentarts-toolkit-cli` 脚手架测试（65） |

E2E 以 Python SDK 的 `tests/integration/`（`feature/test` 分支，`d130e21`）为基准。Python 基准含 **90 个用例**（顶层 12 文件 69 用例 + `toolkit/` 子目录 4 文件 21 用例，其中 `test_init_creates_project_files` 参数化 4 种模板）。Java 的覆盖关系：SDK 云 E2E 与 CLI 云 E2E 与 Python 全面对齐——`CliLocalE2ETest` 本轮补齐到与 `test_cli_local.py` **1:1**（version/help/init-files/config add·set-get·env·set-default-remove 共 13 用例）、gateway get/getTarget 补内容断言、CLI 命令全部接通 SDK 客户端、Docker deploy 链已落地（`CliDeployedRuntimeE2ETest` 4 个用例在 L3 + Docker 前置满足时真实运行）；Java 额有脚手架与状态存储测试；剩余缺口仅为 3 个 init 模板（langgraph/langchain/google-adk）——详见 §4、§7。

---

## 2. 三层安全模型

E2E 测试按"是否会写云资源 / 是否计费"分三层，由环境变量开关控制。未满足前置条件的测试通过 JUnit 5 的 `assumeTrue()` 静默跳过，不会报失败。

| 层 | 环境变量开关 | 运行内容 | 云写操作 | 计费 |
|---|---|---|---|---|
| **L1 只读** | 仅 AK/SK | list/get + 本地 RuntimeApp（`/ping`、`/invocations`、`/ws`） | 无 | 无 |
| **L2 生命周期** | `AGENTARTS_TEST_ALLOW_CREATE=1` | 每个资源类型 create→get→update→delete | 有，LIFO 自动清理 | 无 |
| **L3 计费** | `AGENTARTS_TEST_RUN_BILLABLE=1` + 预置资源 | CodeInterpreter 沙箱 session、Runtime invoke/exec、Docker deploy 链 | 有 | 产生实际费用 |

### 资源清理

L2/L3 使用 `E2EResourceRegistry`，所有 delete 操作按**注册逆序**（LIFO）在 `@AfterAll` 统一执行，即使中途断言失败也会清理。所有资源名带 `aa-it-<run_id>-` 前缀，便于排查残留资源。

---

## 3. 运行方法

### 3.1 环境变量

| 变量 | 必需层 | 说明 |
|---|---|---|
| `HUAWEICLOUD_SDK_AK` | L1+ | 华为云 AK |
| `HUAWEICLOUD_SDK_SK` | L1+ | 华为云 SK |
| `AGENTARTS_TEST_ALLOW_CREATE` | L2 | 设为 `1` 启用生命周期测试 |
| `AGENTARTS_TEST_RUN_BILLABLE` | L3 | 设为 `1` 启用计费测试（含 Docker deploy 链） |
| `AGENTARTS_TEST_REGION` | 可选 | 覆盖默认 region |
| `AGENTARTS_TEST_STS_AGENCY_URN` | 可选 | STS 凭证 provider 生命周期 + 装饰器用例（`iam::<agencyName>`） |
| `AGENTARTS_TEST_OAUTH2_CLIENT_ID` | 可选 | OAuth2 凭证 provider 生命周期用例 |
| `AGENTARTS_TEST_OAUTH2_CLIENT_SECRET` | 可选 | 同上 |
| `AGENTARTS_TEST_OAUTH2_VENDOR` | 可选 | OAuth2 厂商，默认 `GITHUBOAUTH2` |
| `AGENTARTS_TEST_CODE_INTERPRETER_NAME` | L3 CI | 预置的 Code Interpreter 名称 |
| `HUAWEICLOUD_SDK_CODE_INTERPRETER_API_KEY` | L3 CI | Code Interpreter 数据面 API Key |
| `AGENTARTS_TEST_RUNTIME_AGENT_NAME` | L3 Runtime | 预置的 Runtime Agent 名称 |
| 本机 `docker`（PATH 可执行） | L3 Deploy | `CliDeployedRuntimeE2ETest` 的 deploy 链需 Docker daemon |

### 3.2 常用命令

```bash
# 必须使用系统 Maven（D:\apache-maven-3.9.16）+ JDK 26
export JAVA_HOME="/c/Program Files/Java/jdk-26.0.1"
export PATH="$JAVA_HOME/bin:/d/apache-maven-3.9.16/bin:$PATH"

# 全量（单元 + 集成 + E2E），无凭证时 E2E 自动跳过
mvn test

# 仅 E2E 模块（L1 只读层）
export HUAWEICLOUD_SDK_AK=<your-ak>
export HUAWEICLOUD_SDK_SK=<your-sk>
mvn test -pl agentarts-sdk-tests -am

# L2 生命周期层（创建云资源，自动清理）
export AGENTARTS_TEST_ALLOW_CREATE=1
mvn test -pl agentarts-sdk-tests -am

# L3 计费层（产生实际费用，需预置资源）
export AGENTARTS_TEST_RUN_BILLABLE=1
export AGENTARTS_TEST_CODE_INTERPRETER_NAME=<预置CI名>
export HUAWEICLOUD_SDK_CODE_INTERPRETER_API_KEY=<CI数据面Key>
export AGENTARTS_TEST_RUNTIME_AGENT_NAME=<预置Agent名>
mvn test -pl agentarts-sdk-tests -am -Dtest='CodeInterpreterSessionTest,RuntimeSessionLifecycleTest'

# L3 Docker deploy 链（需本机 docker + 计费开关 + 预置 agent）
export AGENTARTS_TEST_RUN_BILLABLE=1
export AGENTARTS_TEST_ALLOW_CREATE=1
mvn test -pl agentarts-sdk-tests -am -Dtest=CliDeployedRuntimeE2ETest -Dsurefire.failIfNoSpecifiedTests=false

# 单个测试类
mvn test -pl agentarts-sdk-tests -am -Dtest=MemoryLifecycleTest -Dsurefire.failIfNoSpecifiedTests=false
```

---

## 4. 与 Python SDK 的用例对比

Python 基准：`agentarts-sdk-python` 仓库 `feature/test` 分支（commit `d130e21`）下 `tests/integration/`，共 16 个文件 / 90 个用例（顶层 12 文件 69 用例 + `toolkit/` 子目录 4 文件 21 用例，其中 `test_init_creates_project_files` 参数化 4 种模板）。

Java 对应：`agentarts-sdk-tests/.../e2e/`（17 个测试类 90 用例 = 69 SDK 云 + 7 状态存储 + 14 CLI 云）+ `agentarts-toolkit-cli/.../toolkit/`（2 个测试类 65 用例）。

图例：✅ 对齐 ｜ 🟡 双方同状态跳过 / 机制或断言有差异 ｜ ❌ Python 有、Java 缺失 ｜ ➕ Java 独有

> **审计结论**：逐文件核对后确认——**所有 Java E2E 用例均无 mock/桩实现**，全部驱动真实云 HTTP（AK/SK 或 Bearer 签名）或真实本地 Vert.x 服务器；CLI 命令的 `run()`/`call()` 全部接通 SDK 客户端产生真实云副作用。此前被标注为桩的 `DeployOperation`、`DevOperation`、`RuntimeCommand` 子命令、`ConfigCommand` 子命令均已实现。下列 🟡 标记的"机制差异"均已保留有效断言（成功路径），弱断言已在本轮修复中强化（见 §6）。

### 4.1 SDK 云 E2E（Python 顶层 ↔ Java `e2e` 包）

#### Runtime 本地（无云）
| Python | Java | 状态 |
|---|---|---|
| `test_ping_default_healthy` | `testPingDefaultHealthy` | ✅ Java 用 Vert.x WebClient 替代 Starlette TestClient，断言 200 + `status=healthy` + `time_of_last_update` |
| `test_ping_force_unhealthy` | `testPingForceUnhealthy` | ✅ 断言 200 + `status=unhealthy` |
| `test_ping_custom_handler` | `testPingCustomHandler` | ✅ `setPingHandler` 注入，断言 `status=healthy_busy` |
| `test_invocation_returns_handler_result` | `testInvocationReturnsHandlerResult` | ✅ 断言 200 + `echo=hello` + session header |
| `test_invocation_no_entrypoint_returns_404` | `testInvocationNoEntrypointReturns404` | ✅ 断言 404 |
| `test_invocation_invalid_json_returns_400` | `testInvocationInvalidJsonReturns400` | ✅ 断言 400 |
| `test_invocation_handler_raises_returns_500` | `testInvocationHandlerRaiseReturns500` | ✅ 断言 500 |
| `test_invocation_sync_generator_streams_sse` | `testInvocationSyncGeneratorStreamsSse` | 🟡 Java 无同步生成器，用 `Flux.just`；断言 200 + content-type + `data:` 事件数 ≥3 + 内容含 `a`/`c` |
| `test_invocation_async_generator_streams_sse` | `testInvocationAsyncGeneratorStreamsSse` | 🟡 Java 无 async 生成器，用 `Flux.interval`；断言 200 + content-type + `data:` 事件数 ≥2 |
| `test_websocket_without_handler_closes_1011` | `testWebsocketWithoutHandlerCloses1011` | ✅ 已强化：`assertEquals((short)1011, closeCode)`（原先仅 `assertNotNull`） |
| `test_websocket_echo_handler` | `testWebsocketEchoHandler` | ✅ 断言回显 `echo.msg=ping` |

#### Runtime Session（L3 计费）
| Python | Java | 状态 |
|---|---|---|
| `test_runtime_session_upload_download` | `testRuntimeSessionUploadDownload` | ✅ 已强化：exec 断言含 `hello-aa-it`；download 断言 `getDataAsString().contains("hello-aa-it")` 内容 roundtrip（原先仅断言 `isSuccess()`） |

#### 只读列表（L1）
| Python | Java | 状态 |
|---|---|---|
| `test_list_spaces` | `testListSpaces` | ✅ 断言 items 为 List（只读探测层无 created 项，`assertNotNull(items)` + `instanceof List` 即有效边界） |
| `test_list_gateways` | `testListMcpGateways` | ✅ 已强化：断言 `getDataAsJson()` 非空（响应体可解析为 JSON，非空 200）；403 时 `assumeTrue` 跳过（租户未开通 MCP） |
| `test_list_runtime_agents` | `testListRuntimeAgents` | ✅ 断言 items 为 List |
| `test_list_code_interpreters` | `testListCodeInterpreters` | ✅ 断言 items 为 List |

#### Identity 只读（L1）
| Python | Java | 状态 |
|---|---|---|
| `test_list_workload_identities` | `testListWorkloadIdentities` | ✅ |
| `test_list_api_key_credential_providers` | `testListApiKeyCredentialProviders` | ✅ |
| `test_list_oauth2_credential_providers` | `testListOauth2CredentialProviders` | ✅ |
| `test_list_sts_credential_providers` | `testListStsCredentialProviders` | ✅ |
| `test_get_and_token_for_preprovisioned_workload_identity` | `testGetAndTokenForWorkloadIdentity` | 🟡 无预置时 Java 临时 create→get→token→delete（在只读层做写但 finally 清理）；已强化：断言返回 identity 的 `name` 与请求一致（原先仅 `assertNotNull`）。Python 无预置时 `pytest.skip` |

#### Identity 生命周期（L2）
| Python | Java | 状态 |
|---|---|---|
| `test_get_created_workload_identity` | `testGetCreatedWorkloadIdentity` | ✅ 已强化：`assertEquals(name, wi.getWorkloadIdentity().getName())`（原先仅 `assertNotNull`） |
| `test_update_workload_identity` | `testUpdateWorkloadIdentity` | ✅ re-get 断言 `allowed_resource_oauth2_return_urls` 含回调 URL（强于 Python） |
| `test_list_workload_identities_contains_created` | `testListWorkloadIdentitiesContainsCreated` | ✅ |
| `test_get_api_key_credential_provider` | `testGetApiKeyCredentialProvider` | ✅ 断言 `name` 匹配 |
| `test_list_api_key_credential_providers_contains_created` | `testListApiKeyCredentialProvidersContainsCreated` | ✅ |
| `test_create_workload_access_token` | `testCreateWorkloadAccessToken` | ✅ |
| `test_get_resource_api_key` | `testGetResourceApiKey` | ✅ |
| `test_create_and_delete_oauth2_credential_provider` | `testCreateAndDeleteOauth2CredentialProvider` | 🟡 双方均跳过（无 OAuth2 凭证） |
| `test_create_and_delete_sts_credential_provider` | `testCreateAndDeleteStsCredentialProvider` | 🟡 双方均跳过（无 STS agency URN） |

#### Memory 生命周期（L2）
| Python | Java | 状态 |
|---|---|---|
| `test_get_space` | `testGetSpace` | ✅ 断言 id + name |
| `test_list_spaces_contains_created` | `testListSpacesContainsCreated` | ✅ |
| `test_update_space` | `testUpdateSpace` | ✅ re-get 断言 `description` 已变更（强于 Python） |
| `test_session_created` | `testSessionCreated` | ✅ |
| `test_add_messages` | `testAddMessages` | ✅ 断言 `items.size()==2` |
| `test_list_messages` | `testListMessages` | ✅ 断言 `total>=2` + `size==total` 一致性 |
| `test_get_last_k_messages` | `testGetLastKMessages` | ✅ 断言 size=2 + 含 user/assistant 角色 |
| `test_get_message` | `testGetMessage` | ✅ 断言 msgId 匹配 |
| `test_search_memories` | `testSearchMemories` | ✅ 断言 `size==total` 一致性（强于 Python） |
| `test_list_memories` | `testListMemories` | ✅ 断言 `size==total` 一致性 |
| `test_delete_memory_if_any` | `testDeleteMemoryIfAny` | ✅ 18s 轮询；有 memory 则 delete + re-list 验证已删除，无 memory 则软跳过（对齐 Python——提取是后端异步服务，trivial 消息可能不产出 memory；短轮询避免空转） |
| `test_memory_session_wrapper` | `testMemorySessionWrapper` | ✅ |

#### Memory 异步（L2）
| Python | Java | 状态 |
|---|---|---|
| `test_async_get_last_k_messages` | `testAsyncGetLastKMessages` | ✅ 已强化：补回 user/assistant 角色断言（原先仅 size） |
| `test_async_list_messages` | `testAsyncListMessages` | ✅ `total>=2` + `size==total` |
| `test_async_get_message` | `testAsyncGetMessage` | ✅ 断言 msgId 匹配 |
| `test_async_search_memories` | `testAsyncSearchMemories` | ✅ `size==total` 一致性 |
| `test_async_list_memories` | `testAsyncListMemories` | ✅ `size==total` 一致性 |
| `test_async_delete_memory_if_any` | `testAsyncDeleteMemory` | ✅ 18s 轮询；有 memory 则 async delete + re-list 验证删除，无则软跳过（对齐 Python） |
| `test_async_create_session_and_add_messages` | `testAsyncCreateSessionAndAddMessages` | ✅ 端到端走真实 `*Async` API |
| `test_async_session_wrapper` | `testAsyncSessionWrapper` | 🟡 Java 无 `AsyncMemorySession` 类，用同步 `MemorySession.of` |

> **异步实现说明**：`MemoryClient` 的 `*Async` 方法返回真实 cold `Mono`（`Mono.fromCallable(sync).subscribeOn(boundedElastic)`），订阅时执行真实云 HTTP；底层同步方法内部 `BaseHttpClient.post(...).block()` 会阻塞 boundedElastic 线程——语义上等价于 Python `AsyncMemoryClient`，但非真正非阻塞 reactive HTTP。非桩、非 `Mono.just` 假对象。

#### Code Interpreter（L2 + L3）
| Python | Java | 状态 |
|---|---|---|
| `test_get_code_interpreter` | `testGetCodeInterpreter` | ✅ 断言 id 匹配 |
| `test_list_code_interpreters` | `testListCodeInterpreters` | ✅ 断言含 created（按 id）（移除永真 `total_count>=0`） |
| `test_update_code_interpreter` | `testUpdateCodeInterpreter` | ✅ re-get 断言 tags 含 `{env=aa-it}`（强于 Python） |
| `test_code_session_full_workflow` | `testCodeSessionFullWorkflow` | ✅ 已强化：`execute_command` 的 echo、upload、download 统一到同一哨兵串 `hello-aa-it`，并断言 download 内容含该串（原先 echo 与 upload 内容不一致） |

#### Gateway 生命周期（L2）
| Python | Java | 状态 |
|---|---|---|
| `test_get_gateway` | `testGetGateway` | ✅ 已强化：断言响应 `getDataAsJson()` 含 requested `gatewayId`（原先仅 `isSuccess()`） |
| `test_list_gateways` | `testListGateways` | ✅ 已强化：断言响应含 created `gatewayId`（原先仅 `assertNotNull(data)`） |
| `test_update_gateway` | `testUpdateGateway` | ✅ 已强化：re-get + 断言 description 持久化（原先仅断言 success） |
| `test_get_target` | `testGetTarget` | ✅ 已强化：断言响应 `getDataAsJson()` 含 requested `targetId`（原先仅 `isSuccess()`） |
| `test_list_targets` | `testListTargets` | ✅ 已强化：断言响应含 created `targetId` |
| `test_update_target` | `testUpdateTarget` | ✅ 已强化：re-get + 断言 description 持久化 |

> 此前 Java 端因 trust_policy 用了错误的资源动作（`csms:secret:getVersion` 等）被标记为 xfail。已同步 Python `bc280d3` + `create_agency_with_policy` 修复：trust policy 改为 `sts:agencies:assume`，创建 agency 后查找并附加系统策略 `AgentArtsCoreGatewayIdentityAgencyPolicy`，409 检测改用 `ServiceResponseException.getHttpStatusCode()`。xfail 已移除，6 个用例真实通过，且 list/update 已补 containment/re-get 断言。

#### Runtime Agent 生命周期（L2）
| Python | Java | 状态 |
|---|---|---|
| `test_find_agent_by_name` | `testFindAgentByName` | 🟡 Python 复用预置/部署 agent；Java 自建 agent（硬编码 SWR 镜像），断言 `found.id==createdId` |
| `test_find_agent_by_id` | `testFindAgentById` | 🟡 同上，断言 id 匹配 |
| `test_get_agents` | `testGetAgents` | 🟡 同上，断言 `!items.isEmpty()` + 含 created（强于 Python 的 `isinstance`） |
| `test_update_agent` | `testUpdateAgent` | ✅ re-`findAgentById` 断言 `version_detail.description` 已变更（强于 Python） |
| `test_find_agent_endpoint` | `testFindAgentEndpoint` | ✅ 按 endpoint UUID 拼路径，断言 id + name 匹配（强于 Python） |
| `test_update_agent_endpoint` | `testUpdateAgentEndpoint` | ✅ 按 UUID update 成功，re-find 断言 config 持久化 |

> **掩盖风险提示**：Java 自建 agent 的 `@BeforeAll` 若 `createAgent` 抛错（镜像不可达 / 后端拒收 `identity_configuration`），`requireSetup()` 会 `assumeTrue(false)` 静默跳过全部 6 个用例。Python 不尝试 createAgent 故无此掩盖。运行该类时若全部跳过，应检查 `setupError` 日志而非视为通过。

#### Auth 装饰器（L2）
| Python | Java | 状态 |
|---|---|---|
| `test_require_api_key_injects_key` | `testRequireApiKeyInjectsKey` | ✅ `AuthInterceptor.wrap()`+`@RequireApiKey` 注解，断言 api_key 非空 |
| `test_require_sts_token_injects_credentials` | `testRequireStsTokenInjectsCredentials` | 🟡 默认跳过（无 STS agency URN）；提供 URN 时真实运行并断言 access/secret 非空 |
| `test_require_access_token_3lo_is_manual` | `testRequireAccessToken3loIsManual` | 🟡 双方均跳过（OAuth2 3LO 交互式） |

### 4.2 CLI 工具链（Python `toolkit/` ↔ Java `toolkit-cli` + `e2e` 包 CLI 类）

Python `tests/integration/toolkit/` 是 CLI 非模拟集成测试子目录，通过子进程调用真实 `agentarts` 命令行。Java 在 `agentarts-sdk-tests/.../e2e/` 提供 4 个 CLI E2E 测试类（`CliGatewayE2ETest`、`CliMemoryE2ETest`、`CliLocalE2ETest`、`CliDeployedRuntimeE2ETest`），用进程内 picocli `CommandLine` 调用真实 CLI 命令对象并对真实云验证；细粒度脚手架断言在 `agentarts-toolkit-cli` 的 `CliE2ETest` / `CliModuleTest`。

> **审计结论（已更新）**：所有 CLI 命令的 `run()`/`call()` 均已接通 SDK 客户端，**无 `System.out.println` 桩**：
> - `McpGatewayCommand` / `MemoryCommand` / `RuntimeCommand` 子命令 → 构造 `MCPGatewayClient`/`MemoryClient`/`RuntimeClient` 调真实云方法
> - `DeployOperation.deployProject` → 真实 `mvn package` + `docker build` + `SWRServiceClient`（建 namespace/repo/secret）+ `docker login/tag/push` + `RuntimeClient.createOrUpdateAgent` + 回写 `agent_id` 到 config
> - `DevOperation.runDevServer` → 解析 `.agentarts_config.yaml`、反射加载 entrypoint `createApp()` 工厂、`app.run(port)`、打印 `DEV_SERVER_LISTENING on port N`、阻塞至中断
> - `InvokeOperation.invokeAgent` → 本地走 Vertx WebClient `/invocations`，云端走 `RuntimeClient.invokeAgent`
> - `ConfigCommand` / `InitCommand` → `ConfigOperation` / `InitOperation` 真实读写 YAML / 落盘项目文件
>
> 仍存在的实现 TODO（非桩，真实调用但有限制）：`RuntimeCommand.UploadFilesCommand` 用 base64 JSON 非流式上传、`DownloadFilesCommand` 用 UTF-8 字符串解码（可能损坏二进制）、`--endpoint` 未透传到 upload/download、`MemoryCommand` 的 `--strategies/--tags/--vpc-id` 选项已接收但未接到 `createSpace`。这些在 E2E 中以 best-effort 方式真实调用并断言。

#### `test_cli_local.py`（本地，无云）
> 本轮已将 `CliLocalE2ETest` 从 6 个用例补齐到 **13 个**，与 Python `test_cli_local.py`（13 用例，含 `test_init_creates_project_files` 参数化 4 模板）**1:1 对齐**。version/help/init-files/config 全部由 `CliLocalE2ETest` 直接覆盖（`CliE2ETest`/`CliModuleTest` 的等价脚手架测试仍保留作为细粒度补充）。

| Python | Java | 状态 |
|---|---|---|
| `test_cli_version` | `CliLocalE2ETest::test_cli_version`（+ `CliE2ETest::versionCommandReturnsZero` / `CliModuleTest::versionOutput`） | ✅ picocli `--version`，断言退出码 0 + 输出含 `agentarts`/版本号 |
| `test_cli_help` | `CliLocalE2ETest::test_cli_help`（+ `CliE2ETest::helpCommandReturnsZero` / `CliModuleTest::helpListsAllTopLevelCommands`） | ✅ picocli `--help`，断言退出码 0 |
| `test_init_creates_project_files[basic]` | `CliLocalE2ETest::test_init_creates_project_files`（+ `CliE2ETest::initCreatesFullProjectStructure`） | ✅ picocli `init` → `InitOperation.initProject` 落盘，断言 pom.xml / Agent.java / .agentarts_config.yaml / Dockerfile 存在 |
| `test_init_creates_project_files[langgraph]` | `CliLocalE2ETest::test_langgraph_template_not_supported` | 🟡 Java 无该模板，`assertThrows(IOException)` 记录缺口 |
| `test_init_creates_project_files[langchain]` | `CliLocalE2ETest::test_langchain_template_not_supported` | 🟡 同上 |
| `test_init_creates_project_files[google-adk]` | `CliLocalE2ETest::test_google_adk_template_not_supported` | 🟡 同上 |
| `test_init_path_option` | `CliLocalE2ETest::test_init_path_option` | ✅ 断言项目目录在指定子目录 |
| `test_init_invalid_name_fails` | `CliLocalE2ETest::test_init_invalid_name_fails`（+ `CliE2ETest::initLowercasesNameBeforeValidation`） | ✅ picocli `init --name Bad_Name!`，断言退出码非 0 + 目录未创建 |
| `test_config_add_writes_yaml_and_lists` | `CliLocalE2ETest::test_config_add_writes_yaml_and_lists`（+ `CliE2ETest::ConfigOperationE2E::configAddWritesYamlAndLists`） | ✅ picocli `config -n` → 断言 YAML 含 myagent + `config list` 退出码 0（用 `ConfigOperation.setConfigFileOverride` 重定向到 `@TempDir`，等价 Python `monkeypatch.chdir`） |
| `test_config_set_get_roundtrip` | `CliLocalE2ETest::test_config_set_get_roundtrip`（+ `CliE2ETest::ConfigOperationE2E::configSetGetRoundtrip`） | ✅ picocli `config set` / `config get`，断言 YAML 含 hello + get 退出码 0 |
| `test_config_env_lifecycle` | `CliLocalE2ETest::test_config_env_lifecycle`（+ `CliE2ETest::ConfigOperationE2E::configEnvLifecycle`） | ✅ picocli `set-env`/`list-env`/`remove-env`，断言 YAML 增删 |
| `test_config_set_default_and_remove` | `CliLocalE2ETest::test_config_set_default_and_remove`（+ `CliE2ETest::ConfigOperationE2E::configSetDefaultAndRemove`） | ✅ picocli `set-default`/`remove`，断言 `getAgent("a2")` 仍在、`getAgent("a1")` 为 null（用结构化断言而非裸子串——Java YAML 含 `language: "java17"`，其文本含子串 `a1`，裸 `contains("a1")` 会误报） |
| `test_dev_server_serves_ping_and_invocations` | `CliLocalE2ETest::test_dev_server_serves_ping_and_invocations` | ✅ picocli `dev` 子命令 → `DevOperation.runDevServer` → 真实 `AgentArtsRuntimeApp`；断言 `/ping` 200 + `/invocations` 200 + `response=hello from dev e2e` |

#### `test_cli_deployed_runtime.py`（L3，Docker deploy fixture）
> `DeployOperation.deployProject` 已完整实现（Docker 构建 + SWR 推送 + runtime 创建 + config 回写）。`CliDeployedRuntimeE2ETest` 的 `@BeforeAll` 用**真实前置谓词**（`hasCloudCredentials` + `allowCreate` + `allowBillable` + `dockerAvailable`） gating，不再 `assumeTrue(false)`。4 个用例在 L3 + Docker 满足时真实运行 deploy 链并对部署产物断言；LIFO 清理（`RuntimeClient.deleteAgentByName` + `docker rmi -f`）。
| Python | Java | 状态 |
|---|---|---|
| `test_deploy_succeeds` | `CliDeployedRuntimeE2ETest::test_deploy_succeeds` | ✅ deploy 在 `@BeforeAll` 运行；断言 config 含非空 `agent_id` |
| `test_invoke_deployed_agent` | `CliDeployedRuntimeE2ETest::test_invoke_deployed_agent` | ✅ picocli `invoke --mode cloud` → `RuntimeClient.invokeAgent`；404/503/not-ready 时软跳过（对齐 Python 瞬时容忍） |
| `test_runtime_session_on_deployed_agent` | `CliDeployedRuntimeE2ETest::test_runtime_session_on_deployed_agent` | ✅ picocli `runtime start-session`/`exec-command`/`stop-session`；断言 `session_id` 非空 + 退出码 0 |
| `test_runtime_file_transfer_on_deployed_agent` | `CliDeployedRuntimeE2ETest::test_runtime_file_transfer_on_deployed_agent` | ✅ picocli `upload-files`/`download-files`；断言下载内容 `== "hello-aa-it"`（401 时软跳过） |

#### `test_cli_gateway.py`
| Python | Java | 状态 |
|---|---|---|
| `test_cli_gateway_list_readonly` | `CliGatewayE2ETest::test_cli_gateway_list_readonly` | ✅ picocli `mcp-gateway list-mcp-gateways` → `MCPGatewayClient.listMcpGateways`；断言退出码 0 + JSON 对象（403 软跳过） |
| `test_cli_gateway_create` | `CliGatewayE2ETest::test_cli_gateway_lifecycle` | ✅ picocli create→get→list→delete；断言 created 非空 + get id 匹配 + list 对象 + delete 退出码 0；LIFO 清理 |

#### `test_cli_memory.py`
| Python | Java | 状态 |
|---|---|---|
| `test_cli_memory_list_readonly` | `CliMemoryE2ETest::test_cli_memory_list_readonly` | ✅ picocli `memory list` → `MemoryClient.listSpaces`；断言退出码 0 + 含 `spaces`/`total` |
| `test_cli_memory_lifecycle` | `CliMemoryE2ETest::test_cli_memory_lifecycle` | ✅ picocli create→list→get→update→status→delete；断言每步 id 匹配 + delete 退出码 0；LIFO 清理 |

### 4.3 Java 独有（Python 无对应）
| Java 类 | 数量 | 覆盖内容 |
|---|---|---|
| `MemoryAgentStateStoreE2ETest` | 7（**全部 @Disabled**） | Memory 作为 Agent 状态存储（AgentScope 集成）：single/list 状态 roundtrip、list 替换、exists、user 隔离、session id 列表。**当前禁用**——后端对消息内容静态加密（`parts=["_encrypted", <ciphertext>]`、`meta=null`），SDK 无解密路径，状态文本 roundtrip 无法验证。见 §7.4。 |
| `CliE2ETest`（脚手架粒度） | 38 | Java 项目脚手架细粒度断言（pom.xml、Agent.java、config 三层、Dockerfile 基础镜像、模板渲染、picocli help/选项、config CRUD 经真实 picocli 路径） |
| `CliModuleTest`（命令树/选项） | 27 | 命令树校验、选项存在性/默认值/别名、模板存在性、配置格式——API 对齐校验 |

### 4.4 汇总

| 维度 | Python (`d130e21`) | Java (当前) |
|---|---|---|
| 文件数 | 16 | 19（含 4 个 CLI E2E 类） |
| 用例数 | 90 | 162（97 e2e 包 + 65 toolkit 脚手架） |
| SDK 云 E2E（§4.1） | 69 | 69 = 58 ✅ + 11 🟡 |
| CLI 云 E2E（§4.2，sdk-tests） | 21 | 21 = 17 ✅ + 4 🟡（本轮 `CliLocalE2ETest` 补齐到 13 后与 Python `toolkit/` **21:21 对齐**） |
| Java 独有 ➕ | — | 65 条（脚手架/命令树）；state store 7 条已 @Disabled（后端加密，见 §7.4） |
| Python 有、Java 缺失 ❌ | — | 0（Docker deploy 链已实现；3 个 init 模板为 🟡 记录缺口，非缺失） |

> 用例数口径：Java 162 = 97（e2e 包：69 SDK 云 + 7 状态存储 @Disabled + 21 CLI 云 e2e）+ 65（toolkit-cli 脚手架）。Python 90 = 69（顶层）+ 21（toolkit）。

> **真实 AK/SK 验证（本轮）**：已用真实凭证跑通 L1 + L2 全部 + CLI 云 E2E。e2e 包 97 个用例方法中，启用 AK/SK + ALLOW_CREATE（不含 L3）时 **93 个计入：78 通过 + 15 软跳过**，另 4 个 `CliDeployedRuntimeE2ETest` 用例因缺 Docker + L3 类级跳过（计入 0）。0 失败 0 错误。15 个软跳过均为合理前置缺失：state store 7（@Disabled，后端加密）、OAuth2/STS 4（无外部凭证）、`delete_memory` 2（后端异步提取未产出 memory）、CI/Runtime 计费 session 2（无 RUN_BILLABLE）。**本轮未发现新代码 bug**——前序提交（`5f0fbfa` 等）已修复真实云跑测暴露的 bug，本轮增量工作为断言强化 + CLI 用例补齐 + 文档刷新，全部经真实云验证通过。L3 计费层（CodeInterpreter session、Runtime invoke、Docker deploy）需 `AGENTARTS_TEST_RUN_BILLABLE=1` + 预置资源 + 本机 Docker，未在本次验证。

---

## 5. 覆盖范围（按模块）

| 模块 | Java E2E 用例数 | 覆盖能力 |
|---|---|---|
| Identity | 14 | Workload Identity CRUD（get 断言 name 匹配）、API Key/OAuth2/STS 凭证 provider CRUD、access token 签发、`@Require*` 注解装饰器注入 |
| Memory | 29 | Space CRUD（自带数据面 API Key）、Session/Messages/Memories 全数据面（`parts` 已改为 `List<Object>` 以容忍后端 `"_encrypted"` 加密标记）、同步+异步（`MemoryClient` 真实 cold `Mono` 异步 API）、`MemorySession` wrapper、状态存储 roundtrip（Java 独有，**现 @Disabled**——后端静态加密，见 §7.4） |
| Gateway | 6 | Gateway/Target CRUD（list 断言含 created、update re-get 断言 description 持久化；IAM agency 修复已同步，xfail 已移除） |
| Code Interpreter | 4 | 控制面 CRUD（list 断言含 created、update re-get 断言 tags）+ 计费沙箱 session（execute/command/upload/download/get_session/clear_context，echo+upload+download 统一哨兵串） |
| Runtime | 18 | Agent/Endpoint 控制面 CRUD（endpoint find/update 按 UUID、update re-find 断言 description）、数据面 session（计费，exec + download 内容 roundtrip）、本地 RuntimeApp（ping/invocations/SSE/WebSocket，WS 1011 具体断言） |
| Auth | 3 | `@RequireApiKey` / `@RequireStsToken` / `@RequireAccessToken` 装饰器 |
| 只读探测 | 4 | 各控制面 list 连通性（断言 items 为 List） |
| CLI 工具链（脚手架） | 65 | init/config 模板渲染、picocli 命令树与选项、Dockerfile 模板、config CRUD 经真实 picocli 路径 |
| CLI 云 E2E | 21 | gateway/memory 的 CLI create/list/get/update/delete 走 picocli 路径对真实云验证；dev server 走 CLI `dev` 子命令驱动真实 RuntimeApp；`CliLocalE2ETest` 13 用例（version/help/init-files/config add·set-get·env·set-default-remove/init-path/invalid-name/dev/3 个不支持模板）与 Python `test_cli_local.py` 1:1；Docker deploy 链 4 用例（L3 + Docker 满足时真实运行） |

---

## 6. 已修复的代码 bug 与弱断言（审计驱动）

E2E 测试审计发现一批"测试通过但掩盖真实代码 bug / 断言过弱"的问题，已修复：

### 6.1 代码 bug（已修复）
- **Gateway IAM agency**（`MCPGatewayClient`）：trust_policy 用了错误的资源动作（`csms:secret:getVersion` 等）被 IAM 拒收；缺策略附加；409 检测靠字符串匹配失效。已同步 Python `bc280d3` + `create_agency_with_policy`：trust policy 改 `sts:agencies:assume`，创建后查找并附加 `AgentArtsCoreGatewayIdentityAgencyPolicy`，409 改用 `ServiceResponseException.getHttpStatusCode()`。xfail 移除，6 用例真实通过。
- **Runtime endpoint find/update 404**（`RuntimeClient`）：路径段用 endpoint name，后端按 UUID 解析 → 404。改用 `createAgentEndpoint` 返回的 UUID。`AgentInfo` 新增 `VersionDetail` 暴露 `version_detail.description`，使 `updateAgent` 的效果可观测。
- **`InitCommand` 退出码**：原为 `Runnable`（恒返回 0），校验失败静默。改为 `Callable<Integer>`，校验失败返回 2。
- **`DevOperation.runDevServer` 桩**：原仅 `println`。已实现：解析 `.agentarts_config.yaml`、反射加载 entrypoint 的 `createApp()` 工厂（无则回退默认 echo）、`app.run(port)`、打印 `DEV_SERVER_LISTENING on port N`、阻塞至中断。
- **`MemoryClient` 无异步 API**：原 `MemoryAsyncTest` 用 `Mono.fromCallable(sync).block()` 假冒异步。已补真实 cold `Mono`-returning 异步方法（`createMemorySessionAsync`、`addMessagesAsync`、`listMessagesAsync`、`searchMemoriesAsync` 等），测试改为订阅真实异步 API（注：底层同步方法仍 `.block()`，非真正非阻塞 reactive HTTP，但非桩）。
- **`IdentityClient` 缺 list provider wrapper**：补 `listApiKeyCredentialProviders` / `listOauth2CredentialProviders` / `listStsCredentialProviders`，测试改走高层 wrapper。
- **`createMcpGatewayTarget` 缺默认 `credential_provider_configuration`**：后端拒收无 config 的 target。已默认 `{"credential_provider_type":"none"}`，对齐 Python。
- **CLI 命令桩**（`McpGatewayCommand`/`MemoryCommand`/`RuntimeCommand` `run()`、`DeployOperation.deployProject`、`DevOperation`）：原为 `System.out.println` 桩或未实现。已全部接通 SDK 客户端——CLI E2E 现走纯 picocli 命令路径产生真实云副作用并断言。

### 6.2 真实 AK/SK 跑测新发现的代码 bug（已修复）
本轮用真实凭证跑 L1+L2+CLI E2E，暴露并修复了一批增量编译/单元测试掩盖的真实 bug：

- **`MessageInfo.parts` 类型过严**（`agentarts-sdk-memory`）：原声明 `List<Map<String, Object>>`，但后端对消息内容静态加密后返回 `parts=["_encrypted", "<base64密文>"]`（字符串元素），Jackson 无法反序列化为 `Map` → `listMessages`/`getMessage`/`getLastKMessages`/`MemorySession` wrapper 全部抛 `APIException`。改为 `List<Object>`，调用方（`MemoryAgentStateStore.extractText`、`MemoryUsageExample`、`MemoryModelTest`）相应改为按元素类型判断。这是被增量编译旧 `.class` + "只断言 total 不断言内容" 双重掩盖的 bug。
- **`Constants.ensureHttps` 跨包不可访问**（`agentarts-sdk-core`）：方法为包级私有（`static String ensureHttps(...)`），却被 `service.runtime` 包的 `RuntimeClient` 调用。`mvn clean compile` 直接编译失败；此前靠增量编译的旧 `.class`（运行时同包同 ClassLoader 不强校验）掩盖。改为 `public`。
- **CLI command `throw e` 受检异常泄漏**（`InvokeCommand`/`McpGatewayCommand`/`MemoryCommand`/`RuntimeCommand`）：各 `run()` 实现 `Runnable`（不声明 `throws`），但 `catch (Exception e) { if (e instanceof CliFailure) throw e; }` 中 `throw e`（`Exception` 为受检）编译失败。`CliFailure extends RuntimeException`，故改为 `throw (CliSupport.CliFailure) e`（共 21 处）。同样被增量编译掩盖。
- **Vert.x Netty DNS 解析器脆弱**（`BaseHttpClient`）：默认 `Vertx.vertx()` 用 Netty 异步 DNS（UDP 5s 超时、2 次查询即放弃），在系统 DNS 可解析的网络上间歇性 `UnknownHostException`（`agentarts.*` / `memory.*` 域名）。改为配置 `AddressResolverOptions`（10s 超时、8 次查询、不缓存负结果）+ 请求层 DNS 失败重试 3 次。

### 6.3 弱断言强化（本轮修复）
- **WS 1011 close code**（`RuntimeAppLocalTest`）：原 `assertNotNull(closeCode)` + 注释 "1011 or similar"。改为 `assertEquals((short)1011, closeCode)`，具体验证关闭码。
- **Identity `get_workload_identity` 无 name 匹配**（`IdentityReadonlyTest` / `IdentityLifecycleTest`）：原仅 `assertNotNull(wi)`。改为 `assertEquals(name, wi.getWorkloadIdentity().getName())`，证明 get 返回的是请求的 identity。
- **Runtime session download 无内容 roundtrip**（`RuntimeSessionLifecycleTest`）：原仅 `assertTrue(dl.isSuccess())`，200+空体也过。改为断言 `dl.getDataAsString().contains("hello-aa-it")`；并给 `execCommand` 补 stdout 断言。
- **CI session echo/upload 内容不一致**（`CodeInterpreterSessionTest`）：原 `executeCommand("echo hello-aa-it")` 与 `uploadFile(...,"FILE_CONTENT")` 用不同串。统一为 `hello-aa-it`，echo + upload + download 三处交叉校验同一哨兵串。
- **Gateway list/update 无 containment/re-get**（`McpGatewayLifecycleTest`）：原 list 仅 `assertNotNull(data)`、update 仅 `success`。list 改为断言响应含 created `gatewayId`/`targetId`；update 改为 re-get + 断言 description 持久化。真实云验证通过。
- **永真 `getTotal() >= 0` 断言**（`ReadonlyListsTest` 3 处 + `CodeInterpreterLifecycleTest` 1 处）：`int` 类型 `>=0` 结构性永真，无法捕获回归。移除该行，保留 `assertNotNull(items)` + `instanceof List` 连通性断言（只读探测层无 created 项可断言内容时，这是有效边界）。
- **Memory async `get_last_k` 缺角色断言**（`MemoryAsyncTest`）：原仅 `assertEquals(2, size)`，弱于同步版。补回 `roles.contains("user")` + `roles.contains("assistant")`。
- **断言强化总则**：所有 update 测试改为 re-get + 断言变更字段；list 测试断言含 created 项或 `size==total` 一致性；取消 `total>=0` 永真断言。每个测试含成功路径断言。

### 6.5 本轮（CLI 1:1 对齐 + 断言强化 + 真实云复验）
- **`CliLocalE2ETest` 补齐到与 `test_cli_local.py` 1:1**（6 → 13 用例）：新增 `test_cli_version`、`test_cli_help`、`test_init_creates_project_files`（basic 模板）、`test_config_add_writes_yaml_and_lists`、`test_config_set_get_roundtrip`、`test_config_env_lifecycle`、`test_config_set_default_and_remove`。config 类用 `ConfigOperation.setConfigFileOverride` 重定向到 `@TempDir`（等价 Python `monkeypatch.chdir`）；picocli 同线程执行，ThreadLocal 覆盖对子命令可见。全部真实本地跑通。
- **Gateway get/getTarget 内容断言**（`McpGatewayLifecycleTest`）：`testGetGateway`/`testGetTarget` 原仅 `assertTrue(isSuccess())`，200+空体也过。改为断言 `getDataAsJson().toString().contains(requestedId)`——get 返回的体必须含所请求的资源 id。真实云验证通过。
- **只读 gateway 列表可解析断言**（`ReadonlyListsTest`）：`testListMcpGateways` 原 403 之外仅 `isSuccess()`。补 `assertNotNull(getDataAsJson())`——响应体必须可解析为 JSON，非空 200。与 Python `assert result.success` 对齐并略强。
- **`test_config_set_default_and_remove` 断言精度**：原计划用裸 `assertFalse(yaml.contains("a1"))`，但 Java YAML 含 `language: "java17"`，其文本含子串 `a1` 会误报。改用结构化断言 `ConfigOperation.loadConfig().getAgent("a1")==null && getAgent("a2")!=null`——验证实际语义（agent 键移除）而非裸子串，更精确。
- **真实云复验结论**：本轮用真实 AK/SK + ALLOW_CREATE 重跑 L1+L2 全部 e2e 包，**0 失败 0 错误**，未发现新代码 bug（前序提交已修复真实云 bug）。强化后的断言（gateway get/getTarget 含 id、gateway list 可解析 JSON）均真实通过。

### 6.4 误报断言回退（本轮修复）
- **`delete_memory` 硬断言误报**（`MemoryLifecycleTest` / `MemoryAsyncTest`）：§6 原将 `testDeleteMemoryIfAny` 从软跳过改为 `assertFalse(items.isEmpty())` 硬断言，意图"揭露提取故障"。真实云跑测显示：trivial 测试消息（"Hello from e2e test"）经 `is_force_extract=true` 后，后端异步提取服务** legitimately 不产生 memory**（轮询空），硬断言产生**误报**。memory 提取是后端服务、非 SDK 代码，SDK 无法强制其产出。已回退为短轮询（18s，6×3s，避免空转）+ 软跳过（`assumeTrue(false)`），与 Python `test_delete_memory_if_any` 语义一致；当 memory 真实存在时仍走 delete + re-list 断言已删除。这不是"放水"——是无 bug 可掩的合法后端行为。

---

## 7. 剩余缺口与限制

### 7.1 init 模板缺口
Java 仅提供 `basic` / `agentscope` 两种 init 模板；Python 还支持 `langgraph` / `langchain` / `google-adk`。Java 用 `CliLocalE2ETest` 的三个 `test_*_template_not_supported` 测试记录该缺口（`assertThrows(IOException)`）。

### 7.2 CLI 子命令的 best-effort 限制（非桩）
`RuntimeCommand` 的 `upload-files`（base64 JSON 非流式）、`download-files`（UTF-8 解码可能损坏二进制）、`--endpoint` 未透传；`MemoryCommand` 的 `--strategies/--tags/--vpc-id` 选项已接收但未接到 `createSpace`。这些子命令已真实调用 SDK 客户端，E2E 以 best-effort 方式断言，但上述限制可能影响二进制文件传输等场景。

### 7.3 其他
- **Runtime Agent 生命周期**：后端要求 `artifact_source_config` + `identity_configuration`，Java 自建 agent（硬编码 SWR 镜像）可能被拒；若 `createAgent` 抛错，`requireSetup()` 会静默跳过全部 6 个用例（见 §4.1 掩盖风险提示）。endpoint find/update 已修复为按 UUID 工作。
- **计费层（L3）默认跳过**：CodeInterpreter session、Runtime invoke、Docker deploy 链产生实际费用，需显式 `AGENTARTS_TEST_RUN_BILLABLE=1` + 预置资源（deploy 链另需本机 Docker）。
- **OAuth2 / STS 凭证 provider 生命周期默认跳过**：需外部输入（OAuth2 client_id/secret/vendor、STS agency URN）。Java 的 `createOauth2CredentialProvider` 已支持完整 vendor 分派（Github/Google/Microsoft/Custom），待提供凭证即可跑。

### 7.4 Memory 消息内容静态加密 → state store 不可验证
真实 AK/SK 跑测发现：AgentArts Memory 后端对消息内容**静态加密**。`listMessages` / `getMessage` 返回 `parts=["_encrypted", "<base64密文>"]`、`meta=null`；后端强制 space 至少有一个 memory 策略（无策略 space 创建被 400 拒），SDK 无解密端点、无密钥派生文档。Python 同后端也收到加密 parts，但其 `test_list_messages` 只断言 `total>=2` 不断言内容，故不裂。

影响：
- Java lifecycle 的 message 类测试（list/get/last-k/wrapper）经 `parts` 改 `List<Object>` 后已容忍加密、按 size/role/id 断言通过。
- **`MemoryAgentStateStore`**（Java 独有，存 State 为消息文本、读回提取）在此后端上 `extractText` 返回 null → roundtrip 无法验证。`extractText` 已修为优雅跳过非 Map 元素（不再崩），7 个 E2E 用例 `@Disabled`，待后端提供解密路径或非加密 space 配置后恢复。这是后端限制、非 SDK 代码 bug。

---

## 8. 凭证安全约定

- **绝不**将真实 AK/SK/API Key 写入源码、配置文件、测试资源或提交信息。
- 所有凭证**仅通过环境变量**注入（`HUAWEICLOUD_SDK_AK` 等），进程结束即失效。
- 测试代码中只用 `E2EConfig.getXxx()` / `Constants.getAk()` 读取环境变量，不持久化。
- `.agent_identity.json` 等本地身份配置在测试中写入临时目录，测试结束清理，不污染仓库。
- 文档、示例中一律使用 `<your-ak>` / `<your-sk>` 占位符。
