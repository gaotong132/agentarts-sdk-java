# 端到端（E2E）测试指南

本文件说明 AgentArts Java SDK 端到端测试的**运行方法、三层安全模型、与 Python SDK 的用例对比、覆盖范围与缺口**，以及凭证安全约定。

> ⚠️ **凭证安全**：本文档所有命令中的 AK/SK 均为占位符（`<your-ak>` / `<your-sk>`）。**切勿**将真实凭证写入代码、配置文件、提交到仓库或贴在 issue 中。所有凭证仅通过环境变量注入，进程结束即失效。

> **基准版本**：Python 对比基准为 `agentarts-sdk-python` 仓库 `feature/test` 分支（commit `d130e21`）。如需复现对比，请确保本地 Python 仓库 `git fetch` 到该版本，而非更早的 `2f64a4b`。

---

## 1. 概述

Java SDK 的测试体系共 **693 个测试**，分两层：

| 层次 | 数量 | 说明 |
|---|---|---|
| 单元 + 集成测试 | 552 | 各模块内部，无云凭证即可运行 |
| E2E 测试 | 141 | `agentarts-sdk-tests` 的 `e2e` 包（76）+ `agentarts-toolkit-cli` 测试（65） |

E2E 以 Python SDK 的 `tests/integration/`（`feature/test` 分支，`d130e21`）为基准。Python 基准含 **90 个用例**（顶层 69 + `toolkit/` 子目录 21）。Java 与 Python 的覆盖关系**并非单纯超集**：Java 在 SDK 云 E2E 与 Java 脚手架测试上覆盖更细，但**缺失 Python `toolkit/` 子目录的 CLI 云/Docker 端到端用例**——详见 §4。

---

## 2. 三层安全模型

E2E 测试按"是否会写云资源 / 是否计费"分三层，由环境变量开关控制。未满足前置条件的测试通过 JUnit 5 的 `assumeTrue()` 静默跳过，不会报失败。

| 层 | 环境变量开关 | 运行内容 | 云写操作 | 计费 |
|---|---|---|---|---|
| **L1 只读** | 仅 AK/SK | list/get + 本地 RuntimeApp（`/ping`、`/invocations`、`/ws`） | 无 | 无 |
| **L2 生命周期** | `AGENTARTS_TEST_ALLOW_CREATE=1` | 每个资源类型 create→get→update→delete | 有，LIFO 自动清理 | 无 |
| **L3 计费** | `AGENTARTS_TEST_RUN_BILLABLE=1` + 预置资源 | CodeInterpreter 沙箱 session、Runtime invoke/exec | 有 | 产生实际费用 |

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
| `AGENTARTS_TEST_RUN_BILLABLE` | L3 | 设为 `1` 启用计费测试 |
| `AGENTARTS_TEST_REGION` | 可选 | 覆盖默认 region |
| `AGENTARTS_TEST_STS_AGENCY_URN` | 可选 | STS 凭证 provider 生命周期 + 装饰器用例（`iam::<agencyName>`） |
| `AGENTARTS_TEST_OAUTH2_CLIENT_ID` | 可选 | OAuth2 凭证 provider 生命周期用例 |
| `AGENTARTS_TEST_OAUTH2_CLIENT_SECRET` | 可选 | 同上 |
| `AGENTARTS_TEST_OAUTH2_VENDOR` | 可选 | OAuth2 厂商，默认 `GITHUBOAUTH2` |
| `AGENTARTS_TEST_CODE_INTERPRETER_NAME` | L3 CI | 预置的 Code Interpreter 名称 |
| `HUAWEICLOUD_SDK_CODE_INTERPRETER_API_KEY` | L3 CI | Code Interpreter 数据面 API Key |
| `AGENTARTS_TEST_RUNTIME_AGENT_NAME` | L3 Runtime | 预置的 Runtime Agent 名称 |

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

# 单个测试类
mvn test -pl agentarts-sdk-tests -am -Dtest=MemoryLifecycleTest -Dsurefire.failIfNoSpecifiedTests=false
```

---

## 4. 与 Python SDK 的用例对比

Python 基准：`agentarts-sdk-python` 仓库 `feature/test` 分支（commit `d130e21`）下 `tests/integration/`，共 16 个文件 / 90 个用例（顶层 12 文件 69 用例 + `toolkit/` 子目录 4 文件 21 用例，其中 `test_init_creates_project_files` 参数化 4 种模板）。

Java 对应：`agentarts-sdk-tests/.../e2e/`（13 文件 76 用例）+ `agentarts-toolkit-cli/.../toolkit/`（2 文件 65 用例）。

图例：✅ 对齐 ｜ 🟡 双方同状态跳过 / 机制或断言有差异 ｜ ❌ Python 有、Java 缺失 ｜ ➕ Java 独有

### 4.1 SDK 云 E2E（Python 顶层 ↔ Java `e2e` 包）

#### Runtime 本地（无云）
| Python | Java | 状态 |
|---|---|---|
| `test_ping_default_healthy` | `testPingDefaultHealthy` | ✅ Java 用 Vert.x WebClient 替代 Starlette TestClient |
| `test_ping_force_unhealthy` | `testPingForceUnhealthy` | ✅ |
| `test_ping_custom_handler` | `testPingCustomHandler` | ✅ |
| `test_invocation_returns_handler_result` | `testInvocationReturnsHandlerResult` | ✅ |
| `test_invocation_no_entrypoint_returns_404` | `testInvocationNoEntrypointReturns404` | ✅ |
| `test_invocation_invalid_json_returns_400` | `testInvocationInvalidJsonReturns400` | ✅ |
| `test_invocation_handler_raises_returns_500` | `testInvocationHandlerRaiseReturns500` | ✅ |
| `test_invocation_sync_generator_streams_sse` | `testInvocationSyncGeneratorStreamsSse` | ✅ Java 用 `Flux.just` |
| `test_invocation_async_generator_streams_sse` | `testInvocationAsyncGeneratorStreamsSse` | ✅ Java 用 `Flux.interval` |
| `test_websocket_without_handler_closes_1011` | `testWebsocketWithoutHandlerCloses1011` | ✅ |
| `test_websocket_echo_handler` | `testWebsocketEchoHandler` | ✅ |

#### Runtime Session（L3 计费）
| Python | Java | 状态 |
|---|---|---|
| `test_runtime_session_upload_download` | `testRuntimeSessionUploadDownload` | ✅ 步骤一致 |

#### 只读列表（L1）
| Python | Java | 状态 |
|---|---|---|
| `test_list_spaces` | `testListSpaces` | ✅ |
| `test_list_gateways` | `testListMcpGateways` | ✅ |
| `test_list_runtime_agents` | `testListRuntimeAgents` | ✅ |
| `test_list_code_interpreters` | `testListCodeInterpreters` | ✅ |

#### Identity 只读（L1）
| Python | Java | 状态 |
|---|---|---|
| `test_list_workload_identities` | `testListWorkloadIdentities` | ✅ |
| `test_list_api_key_credential_providers` | `testListApiKeyCredentialProviders` | ✅ |
| `test_list_oauth2_credential_providers` | `testListOauth2CredentialProviders` | ✅ |
| `test_list_sts_credential_providers` | `testListStsCredentialProviders` | ✅ |
| `test_get_and_token_for_preprovisioned_workload_identity` | `testGetAndTokenForWorkloadIdentity` | 🟡 无预置时 Java 临时创建再清理，Python 跳过 |

#### Identity 生命周期（L2）
| Python | Java | 状态 |
|---|---|---|
| `test_get_created_workload_identity` | `testGetCreatedWorkloadIdentity` | ✅ |
| `test_update_workload_identity` | `testUpdateWorkloadIdentity` | ✅ |
| `test_list_workload_identities_contains_created` | `testListWorkloadIdentitiesContainsCreated` | ✅ |
| `test_get_api_key_credential_provider` | `testGetApiKeyCredentialProvider` | ✅ |
| `test_list_api_key_credential_providers_contains_created` | `testListApiKeyCredentialProvidersContainsCreated` | ✅ |
| `test_create_workload_access_token` | `testCreateWorkloadAccessToken` | ✅ |
| `test_get_resource_api_key` | `testGetResourceApiKey` | ✅ |
| `test_create_and_delete_oauth2_credential_provider` | `testCreateAndDeleteOauth2CredentialProvider` | 🟡 双方均跳过（无 OAuth2 凭证） |
| `test_create_and_delete_sts_credential_provider` | `testCreateAndDeleteStsCredentialProvider` | 🟡 双方均跳过（无 STS agency URN） |

#### Memory 生命周期（L2）
| Python | Java | 状态 |
|---|---|---|
| `test_get_space` | `testGetSpace` | ✅ |
| `test_list_spaces_contains_created` | `testListSpacesContainsCreated` | ✅ |
| `test_update_space` | `testUpdateSpace` | ✅ |
| `test_session_created` | `testSessionCreated` | ✅ |
| `test_add_messages` | `testAddMessages` | ✅ |
| `test_list_messages` | `testListMessages` | ✅ |
| `test_get_last_k_messages` | `testGetLastKMessages` | ✅ |
| `test_get_message` | `testGetMessage` | ✅ |
| `test_search_memories` | `testSearchMemories` | ✅ |
| `test_list_memories` | `testListMemories` | ✅ |
| `test_delete_memory_if_any` | `testDeleteMemoryIfAny` | ✅ 双方在无 extraction 时软跳过 |
| `test_memory_session_wrapper` | `testMemorySessionWrapper` | ✅ |

#### Memory 异步（L2）
| Python | Java | 状态 |
|---|---|---|
| `test_async_get_last_k_messages` | `testAsyncGetLastKMessages` | ✅ Java 用 `Mono.fromCallable`+`boundedElastic` 模拟 asyncio |
| `test_async_list_messages` | `testAsyncListMessages` | ✅ |
| `test_async_get_message` | `testAsyncGetMessage` | ✅ |
| `test_async_search_memories` | `testAsyncSearchMemories` | ✅ |
| `test_async_list_memories` | `testAsyncListMemories` | ✅ |
| `test_async_delete_memory_if_any` | `testAsyncDeleteMemoryIfAny` | ✅ |
| `test_async_create_session_and_add_messages` | `testAsyncCreateSessionAndAddMessages` | ✅ |
| `test_async_session_wrapper` | `testAsyncSessionWrapper` | 🟡 Java 用同步 `MemorySession.of`（无独立 `AsyncMemorySession` 类） |

#### Code Interpreter（L2 + L3）
| Python | Java | 状态 |
|---|---|---|
| `test_get_code_interpreter` | `testGetCodeInterpreter` | ✅ |
| `test_list_code_interpreters` | `testListCodeInterpreters` | ✅ |
| `test_update_code_interpreter` | `testUpdateCodeInterpreter` | ✅ |
| `test_code_session_full_workflow` | `testCodeSessionFullWorkflow` | ✅ 5 步全一致 |

#### Gateway 生命周期（L2）
| Python | Java | 状态 |
|---|---|---|
| `test_get_gateway` | `testGetGateway` | ✅ IAM agency 修复已同步（`sts:agencies:assume` trust policy + `AgentArtsCoreGatewayIdentityAgencyPolicy` 自动附加） |
| `test_list_gateways` | `testListGateways` | ✅ |
| `test_update_gateway` | `testUpdateGateway` | ✅ |
| `test_get_target` | `testGetTarget` | ✅ |
| `test_list_targets` | `testListTargets` | ✅ |
| `test_update_target` | `testUpdateTarget` | ✅ |

> 此前 Java 端因 trust_policy 用了错误的资源动作（`csms:secret:getVersion` 等）被标记为 xfail。已同步 Python `bc280d3` + `create_agency_with_policy` 修复：trust policy 改为 `sts:agencies:assume`，创建 agency 后查找并附加系统策略 `AgentArtsCoreGatewayIdentityAgencyPolicy`，409 检测改用 `ServiceResponseException.getHttpStatusCode()`。xfail 已移除，6 个用例真实通过。

#### Runtime Agent 生命周期（L2）
| Python | Java | 状态 |
|---|---|---|
| `test_find_agent_by_name` | `testFindAgentByName` | 🟡 Python 复用预置/部署 agent；Java 自建 agent |
| `test_find_agent_by_id` | `testFindAgentById` | 🟡 同上 |
| `test_get_agents` | `testGetAgents` | 🟡 同上 |
| `test_update_agent` | `testUpdateAgent` | 🟡 同上 |
| `test_find_agent_endpoint` | `testFindAgentEndpoint` | 🟡 Java 仅创建 endpoint 未断言 find（API 返回 404） |
| `test_update_agent_endpoint` | `testUpdateAgentEndpoint` | 🟡 Java 仅创建 endpoint 未断言 update |

#### Auth 装饰器（L2）
| Python | Java | 状态 |
|---|---|---|
| `test_require_api_key_injects_key` | `testRequireApiKeyInjectsKey` | ✅ Java 走 `AuthInterceptor.wrap()`+`@RequireApiKey` 注解 |
| `test_require_sts_token_injects_credentials` | `testRequireStsTokenInjectsCredentials` | 🟡 双方均跳过（无 STS agency URN） |
| `test_require_access_token_3lo_is_manual` | `testRequireAccessToken3loIsManual` | 🟡 双方均跳过（OAuth2 3LO 交互式） |

### 4.2 CLI 工具链（Python `toolkit/` ↔ Java `toolkit-cli`）

Python `tests/integration/toolkit/` 是 CLI 非模拟集成测试子目录，通过子进程调用真实 `agentarts` 命令行。Java 在 `agentarts-sdk-tests/.../e2e/` 新增 4 个 CLI E2E 测试类（`CliGatewayE2ETest`、`CliMemoryE2ETest`、`CliLocalE2ETest`、`CliDeployedRuntimeE2ETest`），用进程内 picocli `CommandLine` 调用 CLI 命令并对真实云验证。

> **说明**：Java 端 `McpGatewayCommand`/`MemoryCommand`/`RuntimeCommand` 的 `run()` 目前是 `System.out.println` 桩，未接 SDK 客户端。因此 CLI E2E 测试同时：(a) 走 picocli 命令路径验证 CLI 接口可解析、退出码为 0；(b) 直接调 SDK 客户端（`MCPGatewayClient`/`MemoryClient`）产生真实云副作用并断言。待 CLI 命令接通客户端后可改为纯 CLI 路径。

#### `test_cli_local.py`（本地，无云）
| Python | Java | 状态 |
|---|---|---|
| `test_cli_version` | `CliE2ETest::versionCommandReturnsZero` / `CliModuleTest::versionOutput` | ✅ |
| `test_cli_help` | `CliE2ETest::helpCommandReturnsZero` / `CliModuleTest::helpOutputContainsCommands` | ✅ |
| `test_init_creates_project_files[basic]` | `CliLocalE2ETest::test_init_path_option`（+ `CliE2ETest::initCreatesFullProjectStructure`） | ✅ Java 校验 pom.xml/Agent.java，Python 校验 agent.py/requirements.txt |
| `test_init_creates_project_files[langgraph]` | `CliLocalE2ETest::test_langgraph_template_not_supported` | 🟡 Java 无该模板，测试断言 init 抛 IOException（记录缺口） |
| `test_init_creates_project_files[langchain]` | `CliLocalE2ETest::test_langchain_template_not_supported` | 🟡 同上 |
| `test_init_creates_project_files[google-adk]` | `CliLocalE2ETest::test_google_adk_template_not_supported` | 🟡 同上 |
| `test_init_path_option` | `CliLocalE2ETest::test_init_path_option` | ✅ |
| `test_init_invalid_name_fails` | `CliLocalE2ETest::test_init_invalid_name_fails`（+ `CliE2ETest::initLowercasesNameBeforeValidation`） | ✅ |
| `test_config_add_writes_yaml_and_lists` | `CliE2ETest::addAgentCreatesConfigFile` | 🟡 Java 经 setter 写 YAML，未断言 `config list` CLI |
| `test_config_set_get_roundtrip` | `CliE2ETest::dotNotationConfigAccess` | 🟡 Java 经 setter，未测 `config set/get` CLI |
| `test_config_env_lifecycle` | `CliE2ETest::environmentVariablesCrud` | 🟡 Java 经 setter，未测 `set-env/list-env/remove-env` CLI |
| `test_config_set_default_and_remove` | `CliE2ETest::fullCrudLifecycle` | 🟡 Java 经 setter 做 CRUD+set-default+remove |
| `test_dev_server_serves_ping_and_invocations` | `CliLocalE2ETest::test_dev_server_serves_ping_and_invocations` | 🟡 Java 直接驱动 `AgentArtsRuntimeApp`（`DevOperation` 是桩），未走 `dev` CLI 子命令 |

#### `test_cli_deployed_runtime.py`（L3，Docker deploy fixture）
> Java `DeployOperation.deployProject` 与 `RuntimeCommand` 的 session/exec/upload/download 子命令均为桩（无 Docker 构建/SWR 推送/runtime 创建）。`CliDeployedRuntimeE2ETest` 提供 4 个测试骨架，类级 `assumeTrue(false)` 跳过，断言已就绪，待 deploy 链落地后接通。
| Python | Java | 状态 |
|---|---|---|
| `test_deploy_succeeds` | `CliDeployedRuntimeE2ETest::test_deploy_succeeds` | 🟡 骨架已就位，跳过（deploy 链未实现） |
| `test_invoke_deployed_agent` | `CliDeployedRuntimeE2ETest::test_invoke_deployed_agent` | 🟡 同上 |
| `test_runtime_session_on_deployed_agent` | `CliDeployedRuntimeE2ETest::test_runtime_session_on_deployed_agent` | 🟡 同上 |
| `test_runtime_file_transfer_on_deployed_agent` | `CliDeployedRuntimeE2ETest::test_runtime_file_transfer_on_deployed_agent` | 🟡 同上 |

#### `test_cli_gateway.py`
| Python | Java | 状态 |
|---|---|---|
| `test_cli_gateway_list_readonly` | `CliGatewayE2ETest::test_cli_gateway_list_readonly` | ✅ picocli 路径 + `MCPGatewayClient.listMcpGateways` 验证 |
| `test_cli_gateway_create` | `CliGatewayE2ETest::test_cli_gateway_create` | ✅ picocli 路径 + `MCPGatewayClient.createMcpGateway` + 清理 |

#### `test_cli_memory.py`
| Python | Java | 状态 |
|---|---|---|
| `test_cli_memory_list_readonly` | `CliMemoryE2ETest::test_cli_memory_list_readonly` | ✅ picocli 路径 + `MemoryClient.listSpaces` 验证 |
| `test_cli_memory_lifecycle` | `CliMemoryE2ETest::test_cli_memory_lifecycle` | ✅ picocli create→list→get→update→delete + `MemoryClient` 全生命周期 + 清理 |

### 4.3 Java 独有（Python 无对应）
| Java 类 | 数量 | 覆盖内容 |
|---|---|---|
| `MemoryAgentStateStoreE2ETest` | 7 | Memory 作为 Agent 状态存储（AgentScope 集成）：single/list 状态 roundtrip、list 替换、exists、user 隔离、session id 列表 |
| `CliE2ETest`（脚手架粒度） | 38 | Java 项目脚手架细粒度断言（pom.xml、Agent.java、config 三层、Dockerfile 基础镜像、模板渲染、picocli help/选项） |
| `CliModuleTest`（命令树/选项） | 27 | 命令树校验、选项存在性/默认值/别名、模板存在性、配置格式——API 对齐校验 |

### 4.4 汇总

| 维度 | Python (`d130e21`) | Java (当前) |
|---|---|---|
| 文件数 | 16 | 15 |
| 用例数 | 90 | 141 |
| SDK 云 E2E 对齐 ✅ | — | 60 条 |
| 双方同状态跳过 / 机制差异 🟡 | — | 17 条 |
| Gateway xfail 状态不一致 🔴 | — | 6 条（Python 已修复并移除 xfail，Java 仍 xfail） |
| Python 有、Java 缺失 ❌ | — | 13 条（8 CLI 云/Docker + 3 模板 + init 路径 + dev server） |
| Java 独有 ➕ | — | 72 条（7 状态存储 + 65 脚手架/命令树） |

---

## 5. 覆盖范围（按模块）

| 模块 | Java E2E 用例数 | 覆盖能力 |
|---|---|---|
| Identity | 14 | Workload Identity CRUD、API Key/OAuth2/STS 凭证 provider CRUD、access token 签发、`@Require*` 注解装饰器注入 |
| Memory | 29 | Space CRUD（自带数据面 API Key）、Session/Messages/Memories 全数据面、同步+异步、`MemorySession` wrapper、状态存储 roundtrip（Java 独有） |
| Gateway | 6 | Gateway/Target CRUD（**当前 xfail，待同步 Python 的 IAM agency 修复**） |
| Code Interpreter | 4 | 控制面 CRUD + 计费沙箱 session（execute/command/upload/download/get_session/clear_context） |
| Runtime | 18 | Agent/Endpoint 控制面 CRUD、数据面 session（计费）、本地 RuntimeApp（ping/invocations/SSE/WebSocket） |
| Auth | 3 | `@RequireApiKey` / `@RequireStsToken` / `@RequireAccessToken` 装饰器 |
| 只读探测 | 4 | 各控制面 list 连通性 |
| CLI 工具链（脚手架） | 65 | init/config 模板渲染、picocli 命令树与选项、Dockerfile 模板——**不含 CLI 云/Docker 端到端** |

---

## 6. 已知缺口与限制

### 6.1 Gateway xfail 未同步（最优先）
Python 在 `bc280d3`（IAM agency 修复：从 `CreateAgencyV5Response.agency` 读取 agency_id）与 `3486961`（移除 xfail + gateway 改名）后，gateway 生命周期用例已通过。Java 端 `McpGatewayLifecycleTest` 仍按 trust_policy bug 早退，6 个用例实际未执行。**需将 Python 的 IAM agency 修复同步到 Java `MCPGatewayClient` / `IdentityServiceClient`，并移除 xfail 早退逻辑。**

### 6.2 CLI 云/Docker 端到端缺失
Python `tests/integration/toolkit/` 通过子进程对真实 `agentarts` CLI 跑 init→config→deploy（Docker 构建 + SWR 推送）→invoke→destroy，以及 gateway/memory 的 CLI CRUD。Java `agentarts-toolkit-cli` 测试仅覆盖脚手架与命令树解析，**未对 Docker 或云执行任何 CLI 命令**。缺失 8 个用例：
- `test_deploy_succeeds`、`test_invoke_deployed_agent`、`test_runtime_session_on_deployed_agent`、`test_runtime_file_transfer_on_deployed_agent`
- `test_cli_gateway_list_readonly`、`test_cli_gateway_create`
- `test_cli_memory_list_readonly`、`test_cli_memory_lifecycle`

### 6.3 模板与本地 CLI 用例缺失
- Java 无 `langgraph` / `langchain` / `google-adk` 三种 init 模板（Python 有）。
- Java 无 `init -p` 路径选项行为测试。
- Java 无 `dev` server 子进程测试（仅校验选项存在）。

### 6.4 其他
- **Runtime Agent 生命周期**：后端要求 `artifact_source_config` + `identity_configuration`，Java 自建 agent 可能被拒，各测试跳过断言。
- **计费层（L3）默认跳过**：CodeInterpreter session、Runtime invoke 产生实际费用，需显式 `AGENTARTS_TEST_RUN_BILLABLE=1` + 预置资源。
- **OAuth2 / STS 凭证 provider 生命周期默认跳过**：需外部输入（OAuth2 client_id/secret/vendor、STS agency URN）。

---

## 7. 凭证安全约定

- **绝不**将真实 AK/SK/API Key 写入源码、配置文件、测试资源或提交信息。
- 所有凭证**仅通过环境变量**注入（`HUAWEICLOUD_SDK_AK` 等），进程结束即失效。
- 测试代码中只用 `E2EConfig.getXxx()` / `Constants.getAk()` 读取环境变量，不持久化。
- `.agent_identity.json` 等本地身份配置在测试中写入临时目录，测试结束清理，不污染仓库。
- 文档、示例中一律使用 `<your-ak>` / `<your-sk>` 占位符。
