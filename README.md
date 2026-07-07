# Huawei Cloud AgentArts SDK for Java

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-17%2B-brightgreen.svg)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-3.9%2B-orange.svg)](https://maven.apache.org/)
[![Tests](https://img.shields.io/badge/Tests-702-brightgreen.svg)]()

Build, deploy and manage AI agents with Huawei Cloud capabilities.

## Overview

AgentArts Java SDK is a comprehensive toolkit for developing, deploying, and managing AI agents on Huawei Cloud. It integrates natively with [agentscope-java](https://github.com/agentscope-ai/agentscope-java) (Reactor-based agent framework). See also the [Python SDK](https://github.com/huaweicloud/agentarts-sdk-python).

### Key Features

- **V11-HMAC-SHA256 Signing** — Full Java implementation of Huawei Cloud V11 signer with HKDF key derivation; dual signing mode support (V11 + SDK-HMAC-SHA256); `@RequireAccessToken` / `@RequireApiKey` / `@RequireStsToken` auth annotations with JDK Proxy interceptor
- **Vert.x Runtime Server** — HTTP server exposing `POST /invocations` (JSON + SSE streaming), `GET /ping` (health check with HEALTHY/HEALTHY_BUSY/UNHEALTHY), `WS /ws` (WebSocket); Semaphore-based concurrency control; `RequestContext` + `AgentArtsRuntimeContext` (7-field ThreadLocal with leak prevention)
- **Cloud Identity** — Workload identity CRUD, access token issuance (forUserId/forJwt), three credential provider types (OAuth2/API Key/STS), resource token retrieval, `TokenPoller` for async OAuth2 flows, `.agent_identity.json` local bootstrap
- **Memory Service** — Dual-plane client (AK/SK control plane + API Key data plane); space/session/message/memory CRUD; `MemorySession` convenience wrapper; OpenAPI "parts" format serialization; search with filters (query, topK, minScore)
- **Code Interpreter** — Sandboxed code execution client; `CodeSession` AutoCloseable context manager; execute code/commands, upload/download files, install packages, clear context
- **MCP Gateway** — Gateway and target CRUD management via AK/SK signed requests (10 API methods)
- **Runtime Client** — Dual-plane client for agent lifecycle management: control plane (AK/SK) with 11 methods (agent CRUD + endpoint CRUD), data plane with 6 methods (invoke, exec, upload, download, session start/stop); `LocalRuntimeClient` for local development
- **agentscope-java Integration** — `AgentscopeRuntimeHost` (RequestContext→RuntimeContext bridge), `MemoryAgentStateStore` (AgentStateStore 8 methods), `MCPGatewayTool` + `CodeInterpreterTool` (AgentTool implementations), `MessageConverter` (3 bidirectional message type pairs)
- **CLI Toolkit** — Picocli-based CLI with 9 top-level commands and 24 subcommands: `init`, `config` (8 subcommands), `dev`, `deploy`, `invoke`, `destroy`, `runtime` (6 subcommands), `mcp-gateway` (10 subcommands), `memory` (6 subcommands)
- **Spring Boot Starter** — AutoConfiguration for `AgentArtsRuntimeApp`, `@ConfigurationProperties` binding (`agentarts.*`), Actuator HealthIndicator (UP/busy/DOWN)
- **E2E Test Suite** — 69 end-to-end tests with three-tier safety model (read-only / lifecycle / billable): Identity (14), Memory (20), MCP Gateway (6), Code Interpreter (4), Runtime (18), Auth (3), Read-only lists (4)
- **Reactive Architecture** — Project Reactor (`Mono`/`Flux`) for async HTTP and SSE streaming, aligned with agentscope-java's reactive model; Reactor `Schedulers.boundedElastic()` for async memory operations

## Module Structure

```
agentarts-sdk-java/
├── agentarts-sdk-bom/                   # Bill of Materials (dependency versions)
├── agentarts-sdk-core/                  # Constants, V11Signer, annotations, config, exceptions
├── agentarts-sdk-service/               # BaseHttpClient, IdentityServiceClient, IAMServiceClient, SWRServiceClient, RuntimeClient
├── agentarts-sdk-runtime/               # Vert.x HttpServer: /invocations, /ping, /ws, SSE, concurrency control
├── agentarts-sdk-identity/              # IdentityClient, auth annotations, TokenPoller, .agent_identity.json
├── agentarts-sdk-memory/                # MemoryClient (dual-plane: control + data), MemorySession
├── agentarts-sdk-tools/                 # CodeInterpreterClient, CodeSession
├── agentarts-sdk-mcpgateway/            # MCPGatewayClient, Gateway/Target/ToolCall models
├── agentarts-sdk-integration-agentscope/# agentscope bridge: RuntimeHost, MemoryAgentStateStore, AgentTool extensions
├── agentarts-toolkit-cli/               # Picocli CLI: init, dev, config, deploy, invoke, destroy, runtime, memory
├── agentarts-spring-boot-starter/       # Spring Boot AutoConfiguration + Properties + HealthIndicator
├── agentarts-sdk-all/                   # Aggregator (all modules)
├── agentarts-sdk-examples/              # Example projects (basic-runtime, agentscope-integration)
└── agentarts-sdk-tests/                 # Cross-module integration & e2e tests (69 e2e + 15 integration)
```

## Documentation

### SDK 用户指南（中文）

| 文档 | 说明 |
|---|---|
| [环境变量配置](docs/cn/sdk_user_guide/environment_variables.md) | 所有环境变量、优先级链、Java 常量对照表 |
| [运行时 SDK](docs/cn/sdk_user_guide/runtime_user_guide.md) | AgentArtsRuntimeApp 服务端 + RuntimeClient 双平面客户端 + LocalRuntimeClient 本地开发 |
| [Memory SDK](docs/cn/sdk_user_guide/memory_user_guide.md) | MemoryClient 双平面架构、MemorySession、完整 API（25+ 方法） |
| [Code Interpreter SDK](docs/cn/sdk_user_guide/tools_user_guide.md) | CodeInterpreterClient、CodeSession、17 种操作方法 |
| [MCP Gateway SDK](docs/cn/sdk_user_guide/mcp_gateway_user_guide.md) | MCPGatewayClient 网关/目标 CRUD、RequestResult |
| [Agent Identity SDK](docs/cn/sdk_user_guide/agent_identity_guide.md) | IdentityClient、注解模式、OAuth2/API Key/STS 三种凭证 |

### 测试指南（中文）

| 文档 | 说明 |
|---|---|
| [E2E 测试指南](docs/cn/e2e_testing_guide.md) | 三层安全模型、运行方法、与 Python SDK 的用例逐条对比、覆盖范围、凭证安全约定 |

### CLI 工具指南（中文）

| 文档 | 命令 |
|---|---|
| [init](docs/cn/toolkit_user_guide/init.md) | 初始化项目 |
| [config](docs/cn/toolkit_user_guide/config.md) | 配置管理（9 个子命令） |
| [dev](docs/cn/toolkit_user_guide/dev.md) | 本地开发服务器 |
| [deploy](docs/cn/toolkit_user_guide/deploy.md) | 云端/本地部署 |
| [invoke](docs/cn/toolkit_user_guide/invoke.md) | 调用 Agent |
| [destroy](docs/cn/toolkit_user_guide/destroy.md) | 销毁 Agent |
| [runtime](docs/cn/toolkit_user_guide/runtime_cli.md) | 运行时管理（6 个子命令） |
| [mcp-gateway](docs/cn/toolkit_user_guide/mcp_gateway_cli.md) | MCP 网关管理（10 个子命令） |

## Quick Start

### Prerequisites

- JDK 17+
- Maven 3.9+
- Huawei Cloud account with AK/SK credentials

### 1. Add Dependencies

```xml
<dependency>
    <groupId>com.huaweicloud.agentarts</groupId>
    <artifactId>agentarts-sdk-runtime</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### 2. Create Your Agent

```java
import com.huaweicloud.agentarts.sdk.core.annotation.Entrypoint;
import com.huaweicloud.agentarts.sdk.core.annotation.Ping;
import com.huaweicloud.agentarts.sdk.core.PingStatus;
import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;

import java.util.Map;

public class MyAgent {

    private final AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();

    @Entrypoint
    public Map<String, Object> handle(Map<String, Object> payload) {
        String message = (String) payload.get("message");
        // Your agent logic here
        return Map.of("result", "Hello from Java agent: " + message);
    }

    @Ping
    public PingStatus healthCheck() {
        return PingStatus.HEALTHY;
    }

    public static void main(String[] args) {
        MyAgent agent = new MyAgent();
        agent.app.run(8080);
    }
}
```

### 3. Run Locally

```bash
# Using CLI (when toolkit-cli is built)
agentarts dev

# Or run directly
mvn compile exec:java -Dexec.mainClass="MyAgent"
```

### 4. Test

```bash
# Health check
curl http://localhost:8080/ping
# {"status":"Healthy","time_of_last_update":1719667200}

# Invoke
curl -X POST http://localhost:8080/invocations \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello!"}'
# {"result":"Hello from Java agent: Hello!"}
```

## agentscope-java Integration

```xml
<dependency>
    <groupId>com.huaweicloud.agentarts</groupId>
    <artifactId>agentarts-sdk-integration-agentscope</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```java
import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.tool.Toolkit;
import com.huaweicloud.agentarts.sdk.integration.agentscope.tool.MCPGatewayTool;
import com.huaweicloud.agentarts.sdk.integration.agentscope.tool.CodeInterpreterTool;
import com.huaweicloud.agentarts.sdk.integration.agentscope.state.MemoryAgentStateStore;
import com.huaweicloud.agentarts.sdk.integration.agentscope.runtime.AgentscopeRuntimeHost;
import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;

// Register AgentArts tools (MCP Gateway, Code Interpreter) as agentscope AgentTools
Toolkit toolkit = new Toolkit();
toolkit.registerAgentTool(new MCPGatewayTool(gatewayClient));
toolkit.registerAgentTool(new CodeInterpreterTool(interpreterClient));

// Use AgentArts Memory as the agent's state store (needs spaceId)
OpenAIChatModel model = OpenAIChatModel.builder()
    .apiKey(System.getenv("OPENAI_API_KEY")).modelName("gpt-4o").build();

ReActAgent agent = ReActAgent.builder()
    .name("my-agent").model(model).toolkit(toolkit)
    .stateStore(new MemoryAgentStateStore(memoryClient, spaceId))
    .build();

// Host the agent behind AgentArts Runtime — POST /invocations dispatches to it
AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
new AgentscopeRuntimeHost(app, (payload, ctx) -> {
    String message = (String) payload.getOrDefault("message", "");
    var reply = agent.call(message, ctx).block();
    return java.util.Map.of("reply", reply != null ? reply.getTextContent() : "");
});
app.run(8080);
```

## Spring Boot Starter

For Spring Boot 3.x applications, use the optional starter for auto-configuration:

```xml
<dependency>
    <groupId>com.huaweicloud.agentarts</groupId>
    <artifactId>agentarts-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```yaml
# application.yml
agentarts:
  region: cn-southwest-2
  runtime:
    port: 8080
    max-concurrency: 15
    auto-start: true
  memory:
    api-key: ${AGENTARTS_MEMORY_API_KEY}
    space-id: ${AGENTARTS_MEMORY_SPACE_ID}
```

The starter auto-configures `AgentArtsRuntimeApp` and provides an Actuator health indicator at `/actuator/health`.

## Examples

The `agentarts-sdk-examples` module contains runnable example applications:

### Basic Runtime Example

A standalone agent using the Vert.x runtime with sync and SSE streaming support:

```bash
mvn compile exec:java -pl agentarts-sdk-examples \
  -Dexec.mainClass="com.huaweicloud.agentarts.examples.BasicRuntimeExample"

# Test:
curl http://localhost:8080/ping
curl -X POST http://localhost:8080/invocations \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello!"}'
```

### agentscope Integration Example

Builds a `ReActAgent` with an OpenAI-compatible model, `@Tool`-annotated tools, optional `MemoryAgentStateStore` persistence, streaming events, and HTTP Runtime hosting via `AgentscopeRuntimeHost`:

```bash
export AGENTARTS_MEMORY_API_KEY=your-api-key
export AGENTARTS_MEMORY_SPACE_ID=your-space-id

mvn compile exec:java -pl agentarts-sdk-examples \
  -Dexec.mainClass="com.huaweicloud.agentarts.examples.AgentScopeIntegrationExample"
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|---|---|---|
| `HUAWEICLOUD_SDK_AK` | Access Key ID | — |
| `HUAWEICLOUD_SDK_SK` | Secret Access Key | — |
| `HUAWEICLOUD_SDK_REGION` | Huawei Cloud region | `cn-southwest-2` |
| `AGENTARTS_CONTROL_ENDPOINT` | Custom control plane endpoint | auto |
| `AGENTARTS_RUNTIME_DATA_ENDPOINT` | Custom data plane endpoint | — |

### `.agentarts_config.yaml`

```yaml
default_agent: my-agent
agents:
  my-agent:
    base:
      name: my-agent
      entrypoint: com.example.MyAgent
      region: cn-southwest-2
      language: java17
      base_image: eclipse-temurin:17-jre
    swr_config:
      organization: my-org
      repository: my-agent
    runtime:
      invoke_config:
        protocol: HTTP
        port: 8080
```

## CLI Toolkit

The `agentarts` CLI provides commands for the full agent lifecycle:

```bash
# Initialize a new project
agentarts init -n my-agent -t basic -r cn-southwest-2

# Local development server
agentarts dev -p 8080

# Configure agent settings
agentarts config -n my-agent -e com.example.MyAgent -r cn-southwest-2
agentarts config set base.region cn-north-4
agentarts config set-env OPENAI_API_KEY sk-xxx
agentarts config list

# Deploy to Huawei Cloud
agentarts deploy -a my-agent -m cloud -t v1.0

# Invoke agent
agentarts invoke '{"message": "Hello!"}' -a my-agent -m local -p 8080

# Destroy agent
agentarts destroy -a my-agent -y
```

See also the [Python SDK](https://github.com/huaweicloud/agentarts-sdk-python) for the equivalent CLI toolkit.

## Building from Source

```bash
git clone https://github.com/gaotong132/agentarts-sdk-java.git
cd agentarts-sdk-java
mvn clean install -DskipTests
```

### Run Tests

```bash
# All tests (unit + integration + e2e)
mvn test

# Unit tests only (core modules)
mvn test -pl agentarts-sdk-core,agentarts-sdk-service,agentarts-sdk-runtime

# E2E tests (read-only tier, requires AK/SK)
export HUAWEICLOUD_SDK_AK=<your-ak> HUAWEICLOUD_SDK_SK=<your-sk>
mvn test -pl agentarts-sdk-tests

# E2E tests (full lifecycle, creates real cloud resources, auto-cleaned)
export AGENTARTS_TEST_ALLOW_CREATE=1
mvn test -pl agentarts-sdk-tests

# E2E tests (billable tier — real money; needs pre-provisioned resources)
export AGENTARTS_TEST_RUN_BILLABLE=1
mvn test -pl agentarts-sdk-tests -Dtest='CodeInterpreterSessionTest,RuntimeSessionLifecycleTest'
```

> See the [E2E 测试指南](docs/cn/e2e_testing_guide.md) for the full three-tier safety model, environment variables, and a per-case comparison with the Python SDK. **Never commit real credentials** — inject them via environment variables only.

## Testing

The test suite includes **702 tests** across three layers:

| Layer | Count | Description |
|---|---|---|
| Unit + integration | 552 | V11 signing, HTTP client, Runtime server/client, Identity, Memory, Tools, MCP Gateway, agentscope, Spring Boot, cross-module — no cloud credentials needed |
| Cloud E2E | 90 | `agentarts-sdk-tests` e2e package: 69 SDK cloud + 21 CLI cloud (with AK/SK + `ALLOW_CREATE` 86 run = 78 pass + 8 skip; 4 `CliDeployedRuntime` need Docker + L3) |
| CLI scaffolding | 60 | `agentarts-toolkit-cli` template rendering, picocli command-tree/option parsing, init/config structure (no Docker/cloud) |

Cloud E2E by area: Identity (14), Memory (20), Gateway (6, IAM agency fix synced — xfail removed), Code Interpreter (4), Runtime (18), Auth decorators (3), read-only probes (4), CLI cloud (21: `CliLocal` 13 1:1 with Python + gateway/memory lifecycle + dev server + Docker deploy 4). Cross-validated against the Python SDK `tests/integration/` (`feature/test` branch, commit `d130e21`, 90 cases). `mvn test` without cloud credentials executes 638 tests (0 failures, 2 skipped — e2e cloud classes skip via `assumeTrue`). Full per-case mapping, code-bug fixes driven by the audit (RuntimeClient endpoint UUID, MemoryClient async API, DevOperation, InitCommand exit code), and remaining gaps (langgraph/langchain/google-adk templates) are documented in the [E2E 测试指南](docs/cn/e2e_testing_guide.md).

### E2E Three-Tier Safety Model

| Tier | Switch | What runs | Cloud writes? |
|---|---|---|---|
| **Default (read-only)** | — | list/get + local RuntimeApp | none |
| **Lifecycle** | `AGENTARTS_TEST_ALLOW_CREATE=1` | create→get→update→delete | yes, LIFO teardown-guaranteed |
| **Billable** | `AGENTARTS_TEST_RUN_BILLABLE=1` | code-interpreter sandbox, runtime invoke | real money |

Tests whose prerequisites aren't met are silently skipped via `assumeTrue()` at `@BeforeAll` — they never report as failures.

## Dependency Versions

Key dependencies aligned with [agentscope-java](https://github.com/agentscope-ai/agentscope-java) BOM:

| Dependency | Version | Notes |
|---|---|---|
| Project Reactor | `2025.0.2` | Aligned with agentscope |
| Jackson | `2.21.1` | Aligned with agentscope |
| Vert.x | `4.5.14` | Self-managed (not in agentscope BOM) |
| Huawei Cloud SDK v3 | `3.1.202` | agentidentity, iam, swr |
| agentscope-core | `2.0.0-SNAPSHOT` | provided/optional |
| Spring Boot | `3.3.5` | Optional (for starter) |
| JUnit | `6.0.2` | Testing |

## API Compatibility

The Java SDK provides the same API surface as the [Python SDK](https://github.com/huaweicloud/agentarts-sdk-python):

| Aspect | Coverage |
|---|---|
| V11 Signing | Cross-language golden vector tests (exact signature match) |
| HTTP Endpoints | Status codes (200/400/500/503), SSE format, error JSON structure |
| Header Constants | 4 standard AgentArts runtime headers |
| Context Fields | 7 thread-local context fields (session, request, user, token, OAuth2) |
| Memory API | 15 methods + MemorySession wrapper + 15 model classes |
| Tools API | 17 methods + CodeSession context manager |
| MCP Gateway API | 10 methods (gateway CRUD + target CRUD) |
| Runtime Client API | 17 methods: agent CRUD (7) + endpoint CRUD (4) + data plane (6: invoke, exec, upload, download, sessions) |
| Identity API | Workload identity CRUD + credential providers + access tokens |
| CLI Commands | 9 top-level + 8 config + 6 runtime + 10 mcp-gateway + 6 memory subcommands |
| agentscope Integration | AgentStateStore (8 methods), AgentTool (6 methods x 2), RuntimeContext bridge, MessageConverter (3 pairs) |
| Spring Boot Starter | AutoConfiguration, ConfigurationProperties, HealthIndicator |
| E2E Tests | 69 tests: Identity lifecycle, Memory sync+async, MCP Gateway, Code Interpreter, Runtime local, Auth decorators |

## Python SDK Reference

This Java SDK maintains API compatibility with the [Python AgentArts SDK](https://github.com/huaweicloud/agentarts-sdk-python). When the Python SDK updates, the Java SDK should be synchronized accordingly.

| Item | Value |
|---|---|
| Python SDK Repository | https://github.com/huaweicloud/agentarts-sdk-python |
| Pinned Commit | [`2f64a4b`](https://github.com/huaweicloud/agentarts-sdk-python/commit/2f64a4b) (feature/test) |
| Branch | `feature/test` |
| Last Synced | 2026-07-01 |

To check for Python SDK changes since last sync:

```bash
cd agentarts-sdk-python
git fetch origin
git log 2f64a4b..origin/feature/test --oneline
```

## License

[Apache License 2.0](LICENSE)
