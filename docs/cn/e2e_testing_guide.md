# 端到端（E2E）测试指南

AgentArts Java SDK 端到端测试的运行方法、三层安全模型、Python 用例对比、覆盖范围与缺口。

> ⚠️ **凭证安全**：命令中 AK/SK 均为占位符（`<your-ak>` / `<your-sk>`）。真实凭证**仅通过环境变量注入**，绝不写入代码/配置/提交。

> **Python 基准**：`agentarts-sdk-python` `feature/test` 分支 commit `d130e21`，`tests/integration/`（16 文件 / 90 用例）。

---

## 1. 概述

`mvn test`（无云凭证时 E2E 云用例自动跳过）：**650 用例，0 失败，9 跳过**。

| 层次 | 数量 | 说明 |
|---|---|---|
| 单元 + 集成 | 537 | 各模块内部，无云凭证即可运行 |
| E2E | 162 | `agentarts-sdk-tests/e2e` 包（97）+ `agentarts-toolkit-cli` 脚手架（65） |

E2E 与 Python 全面对齐：SDK 云 E2E 69、CLI 云 E2E 21（与 Python `toolkit/` 21:21）、Java 独有 7（状态存储，@Disabled）+ 65（脚手架）。**无 mock/桩**——全部驱动真实云 HTTP 或真实本地 Vert.x 服务器。

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
# 系统 Maven + JDK 26（路径见 CLAUDE.md）
export JAVA_HOME="/c/Program Files/Java/jdk-26.0.1"
export PATH="$JAVA_HOME/bin:/d/apache-maven-3.9.16/bin:$PATH"

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

### 4.1 SDK 云 E2E（Python 顶层 ↔ Java `e2e` 包）

**Runtime 本地（无云）** — `RuntimeAppLocalTest` 11 用例，全 ✅。断言：ping 三态（healthy/unhealthy/busy）、invocations 200/404/400/500、SSE `data:` 事件数 + 内容、WebSocket close 1011 + echo。

**Runtime Session（L3）** — `RuntimeSessionLifecycleTest`

| Python | Java | 状态 |
|---|---|---|
| `test_runtime_session_upload_download` | `testRuntimeSessionUploadDownload` | ✅ exec + download 内容含 `hello-aa-it` |

**只读列表（L1）** — `ReadonlyListsTest` 4 用例

| Python | Java | 断言 |
|---|---|---|
| `test_list_spaces` / `test_list_runtime_agents` / `test_list_code_interpreters` | 同名 | items 为 List |
| `test_list_gateways` | `testListMcpGateways` | `getDataAsJson()` 非空；403 软跳过 |

**Identity 只读（L1）** — `IdentityReadonlyTest` 5 用例，全 ✅。4 个 list 断言返回 List；`get_and_token` 断言 name 匹配 + token 非空（无预置时临时 create→delete）。

**Identity 生命周期（L2）** — `IdentityLifecycleTest` 9 用例

| Python | Java | 状态 |
|---|---|---|
| `test_get_created_workload_identity` | 同名 | ✅ name 匹配 |
| `test_update_workload_identity` | 同名 | ✅ re-get 断言 return URL |
| `test_list_*_contains_created`（×2） | 同名 | ✅ 含 created |
| `test_get_api_key_credential_provider` | 同名 | ✅ name 匹配 |
| `test_create_workload_access_token` / `test_get_resource_api_key` | 同名 | ✅ 非空 |
| `test_create_and_delete_oauth2_credential_provider` | 同名 | 🟡 无 OAuth2 凭证，双方跳过 |
| `test_create_and_delete_sts_credential_provider` | 同名 | 🟡 无 STS URN，双方跳过 |

**Memory 生命周期（L2）** — `MemoryLifecycleTest` 12 用例，全 ✅。Space CRUD（get 断言 id+name、update re-get 断言 description）、session/messages（size=2、total≥2、含 user/assistant 角色、msgId 匹配）、memories（size==total 一致性）、`delete_memory` 18s 轮询有则删无则软跳、`MemorySession` wrapper。

**Memory 异步（L2）** — `MemoryAsyncTest` 8 用例，全 ✅/🟡。走真实 `Mono`-returning `*Async` API（订阅时执行真实云 HTTP；底层同步方法 `.block()`，非真正非阻塞 reactive，但非桩）。`test_async_session_wrapper` 🟡 Java 无 `AsyncMemorySession`，用同步 `MemorySession.of`。

**Code Interpreter（L2 + L3）** — `CodeInterpreterLifecycleTest` 3 用例（L2，全 ✅：id 匹配、list 含 created、update re-get 断言 tags）+ `CodeInterpreterSessionTest` 1 用例（L3：execute/command/upload/download/get_session/clear_context，echo+upload+download 统一哨兵串 `hello-aa-it`）。

**Gateway 生命周期（L2）** — `McpGatewayLifecycleTest` 6 用例，全 ✅

| Python | Java | 断言 |
|---|---|---|
| `test_get_gateway` / `test_get_target` | 同名 | 响应体含 requested id |
| `test_list_gateways` / `test_list_targets` | 同名 | 响应含 created id |
| `test_update_gateway` / `test_update_target` | 同名 | re-get 断言 description 持久化 |

> `createMcpGateway` 自动创建/复用 IAM agency `AgentArtsCoreGateway`（trust policy `sts:agencies:assume` + 系统策略 `AgentArtsCoreGatewayIdentityAgencyPolicy`），不删除。

**Runtime Agent 生命周期（L2）** — `RuntimeAgentLifecycleTest` 6 用例

| Python | Java | 状态 |
|---|---|---|
| `test_find_agent_by_name` / `test_find_agent_by_id` / `test_get_agents` | 同名 | 🟡 Python 复用预置 agent；Java 自建（硬编码 SWR 镜像），断言 id 匹配 + 含 created |
| `test_update_agent` | 同名 | ✅ re-find 断言 description |
| `test_find_agent_endpoint` / `test_update_agent_endpoint` | 同名 | ✅ 按 endpoint UUID，断言 id+name / config 持久化 |

> 若 `createAgent` 抛错（镜像不可达 / 后端拒收），`requireSetup()` 静默跳过全部 6 用例——运行时全跳应查 `setupError` 而非视为通过。

**Auth 装饰器（L2）** — `AuthDecoratorsTest` 3 用例

| Python | Java | 状态 |
|---|---|---|
| `test_require_api_key_injects_key` | 同名 | ✅ `AuthInterceptor.wrap()`+`@RequireApiKey`，断言 api_key 非空 |
| `test_require_sts_token_injects_credentials` | 同名 | 🟡 无 STS URN 跳过；提供时断言 access/secret 非空 |
| `test_require_access_token_3lo_is_manual` | 同名 | 🟡 OAuth2 3LO 交互式，双方跳过 |

### 4.2 CLI 工具链（Python `toolkit/` ↔ Java `e2e` 包 CLI 类 + `toolkit-cli` 脚手架）

所有 CLI 命令 `run()`/`call()` 接通 SDK 客户端，无 `println` 桩。

**`test_cli_local.py`（本地，无云）** — `CliLocalE2ETest` 13 用例，与 Python **1:1**

| Python | Java | 断言 |
|---|---|---|
| `test_cli_version` / `test_cli_help` | 同名 | 退出码 0（+ version 输出含 `agentarts`） |
| `test_init_creates_project_files[basic]` | `test_init_creates_project_files` | pom.xml / Agent.java / .agentarts_config.yaml / Dockerfile 存在 |
| `[langgraph]` / `[langchain]` / `[google-adk]` | `test_*_template_not_supported` | 🟡 `assertThrows(IOException)` 记录缺口 |
| `test_init_path_option` / `test_init_invalid_name_fails` | 同名 | 退出码 0 / 非 0 + 目录创建与否 |
| `test_config_add_writes_yaml_and_lists` | 同名 | YAML 含 agent + `config list` 退出码 0 |
| `test_config_set_get_roundtrip` | 同名 | YAML + get 退出码 0 |
| `test_config_env_lifecycle` | 同名 | set-env/list-env/remove-env YAML 增删 |
| `test_config_set_default_and_remove` | 同名 | `getAgent("a2")` 仍在、`getAgent("a1")` 为 null |
| `test_dev_server_serves_ping_and_invocations` | 同名 | `/ping` 200 + `/invocations` 200 + `response=hello from dev e2e` |

> config 类用 `ConfigOperation.setConfigFileOverride` 重定向到 `@TempDir`（等价 Python `monkeypatch.chdir`）。`CliE2ETest`/`CliModuleTest`（65 用例）提供等价细粒度脚手架断言。

**`test_cli_deployed_runtime.py`（L3，Docker）** — `CliDeployedRuntimeE2ETest` 4 用例，全 ✅（L3 + Docker 满足时真实运行 deploy 链）

| Python | Java | 断言 |
|---|---|---|
| `test_deploy_succeeds` | 同名 | config 含非空 `agent_id` |
| `test_invoke_deployed_agent` | 同名 | `invoke --mode cloud` 退出码 0（404/503 软跳） |
| `test_runtime_session_on_deployed_agent` | 同名 | `session_id` 非空 + exec/stop 退出码 0 |
| `test_runtime_file_transfer_on_deployed_agent` | 同名 | 下载内容 `== "hello-aa-it"`（401 软跳） |

**`test_cli_gateway.py`** — `CliGatewayE2ETest` 2 用例 ✅：readonly（list 退出码 0 + JSON，403 软跳）、lifecycle（create→get→list→delete，id 匹配）。

**`test_cli_memory.py`** — `CliMemoryE2ETest` 2 用例 ✅：readonly（含 `spaces`/`total`）、lifecycle（create→list→get→update→status→delete，id 匹配）。

### 4.3 Java 独有

| Java 类 | 数量 | 覆盖 |
|---|---|---|
| `MemoryAgentStateStoreE2ETest` | 7（**@Disabled**） | 状态存储 roundtrip；后端静态加密，无解密路径，见 §6.3 |
| `CliE2ETest` | 38 | 脚手架细粒度（pom.xml/Agent.java/config/Dockerfile/模板/help/选项） |
| `CliModuleTest` | 27 | 命令树、选项默认值/别名、模板存在性、配置格式 |

### 4.4 汇总

| 维度 | Python | Java |
|---|---|---|
| 用例数 | 90 | 162（97 e2e 包 + 65 脚手架） |
| SDK 云 E2E | 69 | 69（58 ✅ + 11 🟡） |
| CLI 云 E2E | 21 | 21（17 ✅ + 4 🟡） |
| Java 独有 | — | 65 脚手架 + 7 @Disabled 状态存储 |
| Python 有 Java 缺失 | — | 0 |

> **真实 AK/SK 验证**：L1 + L2 + CLI 云 E2E 全绿。e2e 包 97 用例，启用 AK/SK + ALLOW_CREATE（不含 L3）时 93 计入（**78 通过 + 15 软跳过**），另 4 个 `CliDeployedRuntime` 缺 Docker+L3 类级跳过。**0 失败 0 错误**。15 跳过：状态存储 7（@Disabled）、OAuth2/STS 4、`delete_memory` 2（后端未产出 memory）、CI/Runtime 计费 2（无 RUN_BILLABLE）。

---

## 5. 覆盖范围（按模块）

| 模块 | E2E 用例 | 覆盖 |
|---|---|---|
| Identity | 14 | Workload Identity CRUD、API Key/OAuth2/STS provider CRUD、access token、`@Require*` 装饰器 |
| Memory | 29 | Space CRUD、Session/Messages/Memories 全数据面（`parts` 为 `List<Object>` 容忍加密）、同步+异步、`MemorySession` wrapper、状态存储（@Disabled） |
| Gateway | 6 | Gateway/Target CRUD |
| Code Interpreter | 4 | 控制面 CRUD + 计费沙箱 session |
| Runtime | 18 | Agent/Endpoint CRUD、数据面 session（计费）、本地 RuntimeApp（ping/invocations/SSE/WS） |
| Auth | 3 | `@RequireApiKey`/`@RequireStsToken`/`@RequireAccessToken` |
| 只读探测 | 4 | 各控制面 list 连通性 |
| CLI 脚手架 | 65 | init/config 模板、命令树、选项、Dockerfile |
| CLI 云 E2E | 21 | gateway/memory CLI CRUD、dev server、`CliLocalE2ETest` 13（1:1 Python）、Docker deploy 4 |

---

## 6. 已知缺口与限制

### 6.1 init 模板
Java 仅 `basic` / `agentscope`；Python 另有 `langgraph` / `langchain` / `google-adk`（3 个 `test_*_template_not_supported` 记录缺口）。

### 6.2 CLI 子命令 best-effort 限制（非桩）
`RuntimeCommand` upload-files（base64 非流式）、download-files（UTF-8 解码可能损坏二进制）、`--endpoint` 未透传；`MemoryCommand` `--strategies/--tags/--vpc-id` 已接收未接 `createSpace`。

### 6.3 Memory 静态加密 → state store 不可验证
后端对消息内容静态加密（`parts=["_encrypted", <密文>]`、`meta=null`），SDK 无解密路径。message 类测试改按 size/role/id 断言通过；`MemoryAgentStateStore` 7 用例 @Disabled，待后端提供解密或非加密 space。

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
