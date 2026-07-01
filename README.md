# Huawei Cloud AgentArts SDK for Java

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-17%2B-brightgreen.svg)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-3.9%2B-orange.svg)](https://maven.apache.org/)
[![Tests](https://img.shields.io/badge/Tests-283%20passing-brightgreen.svg)]()

Build, deploy and manage AI agents with Huawei Cloud capabilities.

## Overview

AgentArts Java SDK is a comprehensive toolkit for developing, deploying, and managing AI agents on Huawei Cloud. It integrates natively with [agentscope-java](https://github.com/agentscope/agentscope-java) (Reactor-based agent framework). See also the [Python SDK](https://github.com/huaweicloud/agentarts-sdk-python).

### Key Features

- **Vert.x Runtime** — Lightweight HTTP/WebSocket/SSE server wrapping your agent logic as `/invocations`, `/ping`, `/ws` endpoints
- **agentscope-java Integration** — Native bridge to `ReActAgent`, `AgentStateStore`, `AgentTool`, streaming `Flux<AgentEvent>` via SSE
- **V11-HMAC-SHA256 Signing** — Full Java implementation of Huawei Cloud V11 signer with HKDF key derivation
- **Built-in Tools** — Code Interpreter sandbox, Memory management, MCP Gateway client
- **Cloud Identity** — Workload identity, OAuth2/API Key/STS credential providers via Huawei Cloud AgentIdentity service
- **CLI Toolkit** — Picocli-based CLI for `init`, `dev`, `deploy`, `invoke`, `destroy`
- **Spring Boot Starter** — Auto-configuration, properties binding, and health indicator for Spring Boot 3.x
- **E2E Test Suite** — 69 end-to-end tests ported from Python SDK, covering Identity, Memory, MCP Gateway, Code Interpreter, Runtime, and Auth decorators
- **Reactive Architecture** — Project Reactor (`Mono`/`Flux`) throughout, aligned with agentscope-java's reactive model

### Python SDK Reference

This Java SDK maintains API compatibility with the [Python AgentArts SDK](https://github.com/huaweicloud/agentarts-sdk-python). When the Python SDK updates, the Java SDK should be synchronized accordingly.

| Item | Value |
|---|---|
| Python SDK Repository | https://github.com/huaweicloud/agentarts-sdk-python |
| Pinned Commit | [`1528b3e`](https://github.com/huaweicloud/agentarts-sdk-python/commit/1528b3e2dea727695855291a53ed6f86be63a39b) |
| Branch | `main` |
| Last Synced | 2026-07-01 |

To check for Python SDK changes since last sync:

```bash
cd agentarts-sdk-python
git fetch origin
git log 1528b3e..origin/main --oneline
```

## Module Structure

```
agentarts-sdk-java/
├── agentarts-sdk-bom/                   # Bill of Materials (dependency versions)
├── agentarts-sdk-core/                  # Constants, V11Signer, annotations, config, exceptions
├── agentarts-sdk-service/               # BaseHttpClient, IdentityServiceClient, IAMServiceClient, SWRServiceClient
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
import io.agentscope.core.tool.Toolkit;
import com.huaweicloud.agentarts.sdk.integration.agentscope.*;

// Build agent with AgentArts tools
Toolkit toolkit = new Toolkit();
toolkit.registerAgentTool(new MCPGatewayTool(gatewayClient));
toolkit.registerAgentTool(new CodeInterpreterTool(interpreterClient));

ReActAgent agent = ReActAgent.builder()
    .name("my-agent")
    .model("openai:gpt-4o")
    .toolkit(toolkit)
    .stateStore(new MemoryAgentStateStore(memoryClient))
    .build();

// Host via AgentArts Runtime — SSE streaming from Flux<AgentEvent>
AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
new AgentscopeRuntimeHost(app, agent);
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

Bridges AgentArts Runtime with agentscope's agent pattern, using `MemoryAgentStateStore` for state persistence and `MessageConverter` for message conversion:

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
export HUAWEICLOUD_SDK_AK=... HUAWEICLOUD_SDK_SK=...
mvn test -pl agentarts-sdk-tests

# E2E tests (full lifecycle, creates real cloud resources)
export AGENTARTS_TEST_ALLOW_CREATE=1
mvn test -pl agentarts-sdk-tests
```

## Testing

The test suite includes **283 tests** across three tiers:

| Tier | Count | Description |
|---|---|---|
| Unit tests | 214 | V11 signing (52), HTTP client (12), Runtime (16), Identity (7), Memory (19), Tools (10), MCP Gateway (5), agentscope (35), CLI (27), Spring Boot (16), Cross-module (15) |
| E2E tests | 69 | Identity (14), Memory (20), MCP Gateway (6), Code Interpreter (4), Runtime (18), Auth (3), Read-only lists (4) — ported 1:1 from Python SDK |

### E2E Three-Tier Safety Model

| Tier | Switch | What runs | Cloud writes? |
|---|---|---|---|
| **Default (read-only)** | — | list/get + local RuntimeApp | none |
| **Lifecycle** | `AGENTARTS_TEST_ALLOW_CREATE=1` | create→get→update→delete | yes, teardown-guaranteed |
| **Billable** | `AGENTARTS_TEST_RUN_BILLABLE=1` | code-interpreter sandbox, runtime invoke | real money |

## Dependency Versions

Key dependencies aligned with [agentscope-java](https://github.com/agentscope/agentscope-java) BOM:

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
| Identity API | Workload identity CRUD + credential providers + access tokens |
| CLI Commands | 9 top-level + 8 config + 6 runtime + 10 mcp-gateway + 6 memory subcommands |
| agentscope Integration | AgentStateStore (8 methods), AgentTool (6 methods x 2), RuntimeContext bridge, MessageConverter (3 pairs) |
| Spring Boot Starter | AutoConfiguration, ConfigurationProperties, HealthIndicator |
| E2E Tests | 69 tests: Identity lifecycle, Memory sync+async, MCP Gateway, Code Interpreter, Runtime local, Auth decorators |

## License

[Apache License 2.0](LICENSE)
