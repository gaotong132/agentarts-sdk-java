# Huawei Cloud AgentArts SDK for Java

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-17%2B-brightgreen.svg)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-3.9%2B-orange.svg)](https://maven.apache.org/)
[![Tests](https://img.shields.io/badge/Tests-87%20passing-brightgreen.svg)]()
[![Python Parity](https://img.shields.io/badge/Python%20Parity-P0%20verified-blue.svg)]()

Build, deploy and manage AI agents with Huawei Cloud capabilities — Java edition.

## Overview

AgentArts Java SDK is a 1:1 Java port of the [Python AgentArts SDK](https://github.com/huaweicloud/agentarts-sdk-python), providing a comprehensive toolkit for developing, deploying, and managing AI agents on Huawei Cloud. It integrates natively with [agentscope-java](https://github.com/agentscope/agentscope-java) (Reactor-based agent framework).

### Key Features

- **Vert.x Runtime** — Lightweight HTTP/WebSocket/SSE server wrapping your agent logic as `/invocations`, `/ping`, `/ws` endpoints
- **agentscope-java Integration** — Native bridge to `ReActAgent`, `AgentStateStore`, `AgentTool`, streaming `Flux<AgentEvent>` via SSE
- **V11-HMAC-SHA256 Signing** — Full Java implementation of Huawei Cloud V11 signer with HKDF key derivation
- **Built-in Tools** — Code Interpreter sandbox, Memory management, MCP Gateway client
- **Cloud Identity** — Workload identity, OAuth2/API Key/STS credential providers via Huawei Cloud AgentIdentity service
- **CLI Toolkit** — Picocli-based CLI for `init`, `dev`, `deploy`, `invoke`, `destroy`
- **Reactive Architecture** — Project Reactor (`Mono`/`Flux`) throughout, aligned with agentscope-java's reactive model

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
├── agentarts-spring-boot-starter/       # (Optional) Spring Boot AutoConfiguration + HealthIndicator
├── agentarts-sdk-all/                   # Aggregator (all modules)
├── agentarts-sdk-examples/              # Example projects
└── agentarts-sdk-tests/                 # Cross-module integration & e2e tests
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

## Building from Source

```bash
git clone https://github.com/gaotong132/agentarts-sdk-java.git
cd agentarts-sdk-java
mvn clean install -DskipTests
```

### Run Tests

```bash
# All tests
mvn test

# Core module only (includes V11Signer tests)
mvn test -pl agentarts-sdk-core
```

## Dependency Versions

Key dependencies aligned with [agentscope-java](https://github.com/agentscope/agentscope-java) BOM:

| Dependency | Version | Notes |
|---|---|---|
| Project Reactor | `2025.0.2` | Aligned with agentscope |
| Jackson | `2.21.1` | Aligned with agentscope |
| Vert.x | `4.5.14` | Self-managed (not in agentscope BOM) |
| Huawei Cloud SDK v3 | `3.1.202` | agentidentity, iam, swr |
| agentscope-core | `2.0.0-SNAPSHOT` | provided/optional |
| JUnit | `6.0.2` | Testing |

## Roadmap

### P0 — Core Foundation ✅ Complete

- [x] **P0a** — Core skeleton + V11 Signer (52 tests, including 6 cross-language golden vector tests)
- [x] **P0b** — Service layer: BaseHttpClient, IdentityServiceClient (31 APIs), SWRServiceClient (12 tests)
- [x] **P0c** — Runtime (Vert.x HttpServer) + Identity (IdentityClient, TokenPoller) (23 tests)

**87 tests total**, Python behavioral parity verified for: V11 signing (exact signature match), HTTP endpoints (status codes, SSE format, error formats, session headers), concurrency control, async task tracking.

### P1–P4 — In Progress

- [ ] **P1** — Memory + Tools + MCP Gateway clients (target: ≥30 tests)
- [ ] **P2** — CLI toolkit (Picocli) + templates + deploy (target: ≥20 tests)
- [ ] **P3** — agentscope-java integration: RuntimeHost, MemoryAgentStateStore, AgentTool extensions (target: ≥15 tests)
- [ ] **P4** — Spring Boot Starter + examples + e2e tests (target: ≥130 total tests)

## Python Parity Status

This SDK is a 1:1 port of the Python AgentArts SDK. Each phase includes behavioral verification against the Python implementation:

| Aspect | Verification Method |
|---|---|
| V11 Signing | Cross-language golden vector tests (fixed AK/SK/timestamp → exact signature match) |
| HTTP Endpoints | Status codes (200/400/500/503), SSE format, error JSON structure match Python |
| Header Constants | 4 constants identical to Python `model.py` |
| Context Fields | 7 ThreadLocal fields matching Python ContextVar |
| Config Format | `.agentarts_config.yaml` and `.agent_identity.json` structure identical |
| API Methods | Each Java client method maps 1:1 to its Python counterpart |

## License

[Apache License 2.0](LICENSE)
