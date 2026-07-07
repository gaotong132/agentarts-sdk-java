# 端到端（E2E）测试指南

AgentArts Java SDK 端到端测试的运行方法、三层安全模型、Python 用例对比、覆盖范围与缺口。

> ⚠️ **凭证安全**：命令中 AK/SK 均为占位符（`<your-ak>` / `<your-sk>`）。真实凭证**仅通过环境变量注入**，绝不写入代码/配置/提交。

> **Python 基准**：`agentarts-sdk-python` `feature/test` 分支 commit `d130e21`，`tests/integration/`（16 文件 / 90 用例）。

---

## 1. 概述

`mvn test`（无云凭证时 E2E 云用例自动跳过）：**639 用例，0 失败，2 跳过**。

| 层次 | 数量 | 说明 |
|---|---|---|
| 单元 + 集成 | 553 | 各模块内部（538）+ `CrossModuleIntegrationTest`（15），无云凭证即可运行 |
| E2E | 150 | `agentarts-sdk-tests/e2e` 包（90）+ `agentarts-toolkit-cli` 脚手架（60） |

E2E 与 Python 全面对齐：SDK 云 E2E 69、CLI 云 E2E 21（与 Python `toolkit/` 21:21）、Java 独有 60（脚手架）。**无 mock/桩**——全部驱动真实云 HTTP 或真实本地 Vert.x 服务器。

---

## 2. 三层安全模型

未满足前置条件的用例由 JUnit 5 `assumeTrue()` 静默跳过，不报失败。

| 层 | 开关 | 运行内容 | 云写 | 计费 |
|---|---|---|---|---|
| **L1 只读** | 仅 AK/SK | list/get + 本地 RuntimeApp | 无 | 无 |
| **L2 生命周期** | `AGENTARTS_TEST_ALLOW_CREATE=1` | create→get→update→delete | 有 | 无 |
| **L3 计费** | `AGENTARTS_TEST_RUN_BILLABLE=1` + 预置资源 | CI 沙箱 session、Runtime invoke/exec、Docker deploy | 有 | 有 |

L2/L3 用 `E2EResourceRegistry` 按**注册逆序（LIFO）**在 `@AfterAll` 清理，即使断言失败也执行。资源名带 `aa-it-<run_id>-` 前缀。

---

## 3. 运行方法

### 3.1 环境变量

| 变量 | 层 | 说明 |
|---|---|---|
| `HUAWEICLOUD_SDK_AK` / `_SK` | L1+ | 华为云凭证 |
| `AGENTARTS_TEST_ALLOW_CREATE` | L2 | `1` 启用生命周期 |
| `AGENTARTS_TEST_RUN_BILLABLE` | L3 | `1` 启用计费（含 Docker deploy） |
| `AGENTARTS_TEST_REGION` | 可选 | 覆盖默认 region（`cn-southwest-2`） |
| `AGENTARTS_TEST_STS_AGENCY_URN` | 可选 | STS provider + 装饰器用例（`iam::<agencyName>`） |
| `AGENTARTS_TEST_OAUTH2_CLIENT_ID` / `_SECRET` / `_VENDOR` | 可选 | OAuth2 provider 用例（vendor 默认 `GITHUBOAUTH2`） |
| `AGENTARTS_TEST_CODE_INTERPRETER_NAME` | L3 | 预置 CI 名称 |
| `HUAWEICLOUD_SDK_CODE_INTERPRETER_API_KEY` | L3 | CI 数据面 Key |
| `AGENTARTS_TEST_RUNTIME_AGENT_NAME` | L3 | 预置 Runtime Agent 名称 |
| 本机 `docker` | L3 | deploy 链需 Docker daemon |

### 3.2 常用命令

```bash
# 需 JDK 17+ 与 Maven 3.9+（确认：java -version / mvn -version）
mvn test                                                    # 全量（无凭证 E2E 跳过）
export HUAWEICLOUD_SDK_AK=<ak> HUAWEICLOUD_SDK_SK=<sk>
mvn test -pl agentarts-sdk-tests -am                        # L1 只读
export AGENTARTS_TEST_ALLOW_CREATE=1
mvn test -pl agentarts-sdk-tests -am                        # L2 生命周期
mvn test -pl agentarts-sdk-tests -am -Dtest=MemoryLifecycleTest -Dsurefire.failIfNoSpecifiedTests=false  # 单类
# L3 计费：另设 AGENTARTS_TEST_RUN_BILLABLE=1 + 预置 CI/Agent + Docker
```

---

## 4. 与 Python SDK 的用例对比

图例：✅ 对齐 ｜ 🟡 双方同状态跳过 / 机制差异 ｜ ❌ Python 有 Java 缺失 ｜ ➕ Java 独有

### 4.0 总表（Python ↔ Java ↔ 状态 ↔ 断言）

| Python | Java | 状态 | 断言 |
|---|---|---|---|
| **Runtime 本地**（`RuntimeAppLocalTest`，无云） | | | |
| `test_ping_default_healthy` | `testPingDefaultHealthy` | ✅ | 200 + `status=healthy` + `time_of_last_update` |
| `test_ping_force_unhealthy` | `testPingForceUnhealthy` | ✅ | 200 + `status=unhealthy` |
| `test_ping_custom_handler` | `testPingCustomHandler` | ✅ | `status=healthy_busy` |
| `test_invocation_returns_handler_result` | `testInvocationReturnsHandlerResult` | ✅ | 200 + `echo=hello` + session header |
| `test_invocation_no_entrypoint_returns_404` | `testInvocationNoEntrypointReturns404` | ✅ | 404 |
| `test_invocation_invalid_json_returns_400` | `testInvocationInvalidJsonReturns400` | ✅ | 400 |
| `test_invocation_handler_raises_returns_500` | `testInvocationHandlerRaiseReturns500` | ✅ | 500 |
| `test_invocation_sync_generator_streams_sse` | `testInvocationSyncGeneratorStreamsSse` | 🟡 | 200 + SSE + `data:`≥3 + 含 `a`/`c`（`Flux.just`） |
| `test_invocation_async_generator_streams_sse` | `testInvocationAsyncGeneratorStreamsSse` | 🟡 | 200 + SSE + `data:`≥2（`Flux.interval`） |
| `test_websocket_without_handler_closes_1011` | `testWebsocketWithoutHandlerCloses1011` | ✅ | closeCode==1011 |
| `test_websocket_echo_handler` | `testWebsocketEchoHandler` | ✅ | `echo.msg=ping` |
| **Runtime Session（L3）** | | | |
| `test_runtime_session_upload_download` | `testRuntimeSessionUploadDownload` | ✅ | exec + download 含 `hello-aa-it` |
| **只读列表（L1，`ReadonlyListsTest`）** | | | |
| `test_list_spaces` | `testListSpaces` | ✅ | items 为 List |
| `test_list_gateways` | `testListMcpGateways` | ✅ | `getDataAsJson()` 非空；403 软跳 |
| `test_list_runtime_agents` | `testListRuntimeAgents` | ✅ | items 为 List |
| `test_list_code_interpreters` | `testListCodeInterpreters` | ✅ | items 为 List |
| **Identity 只读（L1，`IdentityReadonlyTest`）** | | | |
| `test_list_workload_identities` | `testListWorkloadIdentities` | ✅ | 返回 List |
| `test_list_api_key_credential_providers` | `testListApiKeyCredentialProviders` | ✅ | 返回 List |
| `test_list_oauth2_credential_providers` | `testListOauth2CredentialProviders` | ✅ | 返回 List |
| `test_list_sts_credential_providers` | `testListStsCredentialProviders` | ✅ | 返回 List |
| `test_get_and_token_for_preprovisioned_workload_identity` | `testGetAndTokenForWorkloadIdentity` | 🟡 | name 匹配 + token 非空（无预置则临时 create→delete） |
| **Identity 生命周期（L2，`IdentityLifecycleTest`）** | | | |
| `test_get_created_workload_identity` | `testGetCreatedWorkloadIdentity` | ✅ | name 匹配 |
| `test_update_workload_identity` | `testUpdateWorkloadIdentity` | ✅ | re-get 断言 return URL |
| `test_list_workload_identities_contains_created` | `testListWorkloadIdentitiesContainsCreated` | ✅ | 含 created |
| `test_get_api_key_credential_provider` | `testGetApiKeyCredentialProvider` | ✅ | name 匹配 |
| `test_list_api_key_credential_providers_contains_created` | `testListApiKeyCredentialProvidersContainsCreated` | ✅ | 含 created |
| `test_create_workload_access_token` | `testCreateWorkloadAccessToken` | ✅ | 非空 |
| `test_get_resource_api_key` | `testGetResourceApiKey` | ✅ | 非空 |
| `test_create_and_delete_oauth2_credential_provider` | `testCreateAndDeleteOauth2CredentialProvider` | 🟡 | 无 OAuth2 凭证，双方跳过 |
| `test_create_and_delete_sts_credential_provider` | `testCreateAndDeleteStsCredentialProvider` | 🟡 | 无 STS URN，双方跳过 |
| **Memory 生命周期（L2，`MemoryLifecycleTest`）** | | | |
| `test_get_space` | `testGetSpace` | ✅ | id + name |
| `test_list_spaces_contains_created` | `testListSpacesContainsCreated` | ✅ | 含 created |
| `test_update_space` | `testUpdateSpace` | ✅ | re-get 断言 description |
| `test_session_created` | `testSessionCreated` | ✅ | id 非空 |
| `test_add_messages` | `testAddMessages` | ✅ | size==2 |
| `test_list_messages` | `testListMessages` | ✅ | total≥2 + size==total |
| `test_get_last_k_messages` | `testGetLastKMessages` | ✅ | size=2 + 含 user/assistant |
| `test_get_message` | `testGetMessage` | ✅ | msgId 匹配 |
| `test_search_memories` | `testSearchMemories` | ✅ | size==total |
| `test_list_memories` | `testListMemories` | ✅ | size==total |
| `test_delete_memory_if_any` | `testDeleteMemoryIfAny` | ✅ | 18s 轮询；有则删 + re-list，无则软跳 |
| `test_memory_session_wrapper` | `testMemorySessionWrapper` | ✅ | last-k size + total |
| **Memory 异步（L2，`MemoryAsyncTest`）** | | | |
| `test_async_get_last_k_messages` | `testAsyncGetLastKMessages` | ✅ | size=2 + 含 user/assistant |
| `test_async_list_messages` | `testAsyncListMessages` | ✅ | total≥2 + size==total |
| `test_async_get_message` | `testAsyncGetMessage` | ✅ | msgId 匹配 |
| `test_async_search_memories` | `testAsyncSearchMemories` | ✅ | size==total |
| `test_async_list_memories` | `testAsyncListMemories` | ✅ | size==total |
| `test_async_delete_memory_if_any` | `testAsyncDeleteMemory` | ✅ | 18s 轮询；有则删，无则软跳 |
| `test_async_create_session_and_add_messages` | `testAsyncCreateSessionAndAddMessages` | ✅ | 走真实 `*Async` API；id + total≥1 |
| `test_async_session_wrapper` | `testAsyncSessionWrapper` | 🟡 | 用同步 `MemorySession.of` |
| **Code Interpreter（L2 + L3）** | | | |
| `test_get_code_interpreter` | `testGetCodeInterpreter` | ✅ | id 匹配 |
| `test_list_code_interpreters` | `testListCodeInterpreters` | ✅ | 含 created（按 id） |
| `test_update_code_interpreter` | `testUpdateCodeInterpreter` | ✅ | re-get 断言 tags `{env=aa-it}` |
| `test_code_session_full_workflow` | `testCodeSessionFullWorkflow` | ✅ | echo+upload+download 含 `hello-aa-it` |
| **Gateway 生命周期（L2，`McpGatewayLifecycleTest`）** | | | |
| `test_get_gateway` | `testGetGateway` | ✅ | 响应体含 gatewayId |
| `test_list_gateways` | `testListGateways` | ✅ | 响应含 created gatewayId |
| `test_update_gateway` | `testUpdateGateway` | ✅ | re-get 断言 description |
| `test_get_target` | `testGetTarget` | ✅ | 响应体含 targetId |
| `test_list_targets` | `testListTargets` | ✅ | 响应含 created targetId |
| `test_update_target` | `testUpdateTarget` | ✅ | re-get 断言 description |
| **Runtime Agent 生命周期（L2，`RuntimeAgentLifecycleTest`）** | | | |
| `test_find_agent_by_name` | `testFindAgentByName` | 🟡 | id 匹配（Java 自建 agent） |
| `test_find_agent_by_id` | `testFindAgentById` | 🟡 | id 匹配 |
| `test_get_agents` | `testGetAgents` | 🟡 | !isEmpty + 含 created |
| `test_update_agent` | `testUpdateAgent` | ✅ | re-find 断言 description |
| `test_find_agent_endpoint` | `testFindAgentEndpoint` | ✅ | 按 UUID，id + name 匹配 |
| `test_update_agent_endpoint` | `testUpdateAgentEndpoint` | ✅ | 按 UUID，config 持久化 |
| **Auth 装饰器（L2，`AuthDecoratorsTest`）** | | | |
| `test_require_api_key_injects_key` | `testRequireApiKeyInjectsKey` | ✅ | api_key 非空 |
| `test_require_sts_token_injects_credentials` | `testRequireStsTokenInjectsCredentials` | 🟡 | 无 URN 跳过；有则 access/secret 非空 |
| `test_require_access_token_3lo_is_manual` | `testRequireAccessToken3loIsManual` | 🟡 | OAuth2 3LO 交互式，双方跳过 |
| **CLI 本地（`CliLocalE2ETest`，无云）** | | | |
| `test_cli_version` | `test_cli_version` | ✅ | 退出码 0 + 输出含 `agentarts` |
| `test_cli_help` | `test_cli_help` | ✅ | 退出码 0 |
| `test_init_creates_project_files[basic]` | `test_init_creates_project_files` | ✅ | pom.xml/Agent.java/config/Dockerfile 存在 |
| `test_init_creates_project_files[langgraph]` | `test_langgraph_template_not_supported` | 🟡 | `assertThrows(IOException)` 缺口 |
| `test_init_creates_project_files[langchain]` | `test_langchain_template_not_supported` | 🟡 | 同上 |
| `test_init_creates_project_files[google-adk]` | `test_google_adk_template_not_supported` | 🟡 | 同上 |
| `test_init_path_option` | `test_init_path_option` | ✅ | 项目在指定子目录 |
| `test_init_invalid_name_fails` | `test_init_invalid_name_fails` | ✅ | 退出码非 0 + 目录未建 |
| `test_config_add_writes_yaml_and_lists` | `test_config_add_writes_yaml_and_lists` | ✅ | YAML 含 agent + list 退出码 0 |
| `test_config_set_get_roundtrip` | `test_config_set_get_roundtrip` | ✅ | YAML + get 退出码 0 |
| `test_config_env_lifecycle` | `test_config_env_lifecycle` | ✅ | set/list/remove-env YAML 增删 |
| `test_config_set_default_and_remove` | `test_config_set_default_and_remove` | ✅ | `getAgent("a2")` 在、`getAgent("a1")` null |
| `test_dev_server_serves_ping_and_invocations` | `test_dev_server_serves_ping_and_invocations` | ✅ | ping 200 + invocations 200 + echo |
| **CLI Deployed Runtime（L3，`CliDeployedRuntimeE2ETest`）** | | | |
| `test_deploy_succeeds` | `test_deploy_succeeds` | ✅ | config 含非空 `agent_id` |
| `test_invoke_deployed_agent` | `test_invoke_deployed_agent` | ✅ | `invoke --mode cloud` 退出码 0（404/503 软跳） |
| `test_runtime_session_on_deployed_agent` | `test_runtime_session_on_deployed_agent` | ✅ | session_id 非空 + exec/stop 退出码 0 |
| `test_runtime_file_transfer_on_deployed_agent` | `test_runtime_file_transfer_on_deployed_agent` | ✅ | 下载内容 `== "hello-aa-it"`（401 软跳） |
| **CLI Gateway（`CliGatewayE2ETest`）** | | | |
| `test_cli_gateway_list_readonly` | `test_cli_gateway_list_readonly` | ✅ | 退出码 0 + JSON（403 软跳） |
| `test_cli_gateway_create` | `test_cli_gateway_lifecycle` | ✅ | create→get→list→delete，id 匹配 |
| **CLI Memory（`CliMemoryE2ETest`）** | | | |
| `test_cli_memory_list_readonly` | `test_cli_memory_list_readonly` | ✅ | 退出码 0 + 含 `spaces`/`total` |
| `test_cli_memory_lifecycle` | `test_cli_memory_lifecycle` | ✅ | create→list→get→update→status→delete，id 匹配 |

> Java 独有（Python 无对应）：`CliE2ETest` 34 + `CliModuleTest` 26（脚手架，60 用例）。

### 4.1 SDK 云 E2E — 补充说明

- **Runtime 本地**：Java 用 Vert.x `WebClient`/`HttpClient` 驱动真实 `AgentArtsRuntimeApp`（Python 用 Starlette `TestClient`）。SSE 用 `Flux.just`/`Flux.interval`（Java 无 sync/async 生成器）。
- **Memory 异步**：`MemoryClient` 的 `*Async` 方法返回真实 cold `Mono`（订阅时执行真实云 HTTP；底层同步方法 `.block()` 阻塞 boundedElastic，非真正非阻塞 reactive，但非桩）。`test_async_session_wrapper` 用同步 `MemorySession.of`（Java 无 `AsyncMemorySession`）。
- **Gateway**：`createMcpGateway` 自动创建/复用 IAM agency `AgentArtsCoreGateway`（trust policy `sts:agencies:assume` + 系统策略 `AgentArtsCoreGatewayIdentityAgencyPolicy`），不删除。
- **Runtime Agent**：Java 自建 agent（硬编码 SWR 镜像），Python 复用预置/部署 agent。若 `createAgent` 抛错（镜像不可达 / 后端拒收），`requireSetup()` 静默跳过全部 6 用例——运行时全跳应查 `setupError` 而非视为通过。

### 4.2 CLI 工具链 — 补充说明

所有 CLI 命令 `run()`/`call()` 接通 SDK 客户端，无 `println` 桩：`McpGatewayCommand`/`MemoryCommand`/`RuntimeCommand` → 对应 `*Client`；`DeployOperation` → mvn package + docker build + SWR + `createOrUpdateAgent` + 回写 `agent_id`；`DevOperation` → 反射加载 entrypoint `createApp()` + `app.run(port)`；`InvokeOperation` → 本地 Vertx WebClient / 云端 `RuntimeClient.invokeAgent`；`ConfigCommand`/`InitCommand` → `ConfigOperation`/`InitOperation`。

- **`CliLocalE2ETest`** 与 Python `test_cli_local.py` **1:1（13:13）**。config 类用 `ConfigOperation.setConfigFileOverride` 重定向到 `@TempDir`（等价 Python `monkeypatch.chdir`）。
- **`CliDeployedRuntimeE2ETest`** 4 用例在 L3 + Docker 满足时真实运行 deploy 链，LIFO 清理（`deleteAgentByName` + `docker rmi -f` + 删除 SWR repo/namespace）。SWR org/repo 随用例自动清理，不残留。
- `CliE2ETest`/`CliModuleTest`（60 用例）提供等价细粒度脚手架断言。

### 4.3 Java 独有

| Java 类 | 数量 | 覆盖 |
|---|---|---|
| `CliE2ETest` | 34 | 脚手架细粒度（pom.xml/Agent.java/config/Dockerfile/模板/help/选项） |
| `CliModuleTest` | 26 | 命令树、选项默认值/别名、模板存在性、配置格式 |

### 4.4 汇总

| 维度 | Python | Java |
|---|---|---|
| 用例数 | 90 | 150（90 e2e 包 + 60 脚手架） |
| SDK 云 E2E | 69 | 69（58 ✅ + 11 🟡） |
| CLI 云 E2E | 21 | 21（17 ✅ + 4 🟡） |
| Java 独有 | — | 60 脚手架 |
| Python 有 Java 缺失 | — | 0 |

> **真实 AK/SK 验证**：L1 + L2 + CLI 云 E2E + L3 Docker deploy 全绿。e2e 包 90 用例：启用 AK/SK + ALLOW_CREATE（不含 L3）时 86 计入（**78 通过 + 8 软跳过**）；另设 `RUN_BILLABLE=1` + Docker 后 `CliDeployedRuntimeE2ETest` 4 用例计入（**3 通过 + 1 软跳过**，file-transfer 401 软跳——IAM-only agent 上传需 bearer token，与 Python 套件同态）。**0 失败 0 错误**。8 跳过：OAuth2/STS 4、`delete_memory` 2（后端未产出 memory）、CI/Runtime 计费 2（无 RUN_BILLABLE）。


---

## 5. 覆盖范围（按模块）

| 模块 | E2E 用例 | 覆盖 |
|---|---|---|
| Identity | 14 | Workload Identity CRUD、API Key/OAuth2/STS provider CRUD、access token、`@Require*` 装饰器 |
| Memory | 20 | Space CRUD、Session/Messages/Memories 全数据面（`parts` 为 `List<Object>` 容忍加密）、同步+异步、`MemorySession` wrapper |
| Gateway | 6 | Gateway/Target CRUD |
| Code Interpreter | 4 | 控制面 CRUD + 计费沙箱 session |
| Runtime | 18 | Agent/Endpoint CRUD、数据面 session（计费）、本地 RuntimeApp（ping/invocations/SSE/WS） |
| Auth | 3 | `@RequireApiKey`/`@RequireStsToken`/`@RequireAccessToken` |
| 只读探测 | 4 | 各控制面 list 连通性 |
| CLI 脚手架 | 60 | init/config 模板、命令树、选项、Dockerfile |
| CLI 云 E2E | 21 | gateway/memory CLI CRUD、dev server、`CliLocalE2ETest` 13（1:1 Python）、Docker deploy 4 |

---

## 6. 已知缺口与限制

### 6.1 init 模板
Java 仅 `basic` / `agentscope`；Python 另有 `langgraph` / `langchain` / `google-adk`（3 个 `test_*_template_not_supported` 记录缺口）。

### 6.2 CLI 子命令 best-effort 限制（非桩）
`RuntimeCommand` download-files（UTF-8 解码可能损坏二进制）；`MemoryCommand` `--strategies/--tags/--vpc-id` 已接收未接 `createSpace`。upload-files 已实现 `application/octet-stream`（单文件）/ `multipart/form-data`（多文件）流式上传，与参考 CLI wire 格式一致。

### 6.3 Memory 消息内容静态加密
后端对消息内容静态加密（`parts=["_encrypted", <密文>]`、`meta=null`），SDK 无解密路径。message 类测试改按 size/role/id 断言通过（不断言密文内容）。`MemoryAgentStateStore` 生产类仍可用（README/示例有引用），但其状态 roundtrip 在此后端上无法 E2E 验证，相关 E2E 用例已移除。

### 6.4 其他
- Runtime Agent 自建（硬编码 SWR 镜像）可能被后端拒收 → 全跳过（见 §4.1）。
- L3 计费层默认跳过，需 `RUN_BILLABLE=1` + 预置资源 + Docker。
- OAuth2/STS provider 用例默认跳过，需外部凭证。

---

## 7. 凭证安全

- 真实 AK/SK/API Key **绝不**写入源码、配置、测试资源或提交。
- 仅通过环境变量注入，进程结束即失效；测试用 `E2EConfig.getXxx()` / `Constants.getAk()` 读取。
- `.agent_identity.json` 等本地配置写入临时目录，测试结束清理。
- 文档/示例一律用 `<your-ak>` / `<your-sk>` 占位符。
