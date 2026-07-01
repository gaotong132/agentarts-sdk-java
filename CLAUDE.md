# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

This project requires **system Maven** (`D:\apache-maven-3.9.16`) and **JDK 26** (`C:\Program Files\Java\jdk-26.0.1`). Do NOT use IntelliJ's bundled Maven.

```bash
# Set environment (required for every shell session)
export JAVA_HOME="/c/Program Files/Java/jdk-26.0.1"
export PATH="$JAVA_HOME/bin:/d/apache-maven-3.9.16/bin:$PATH"

# Full build + test
mvn clean compile test

# Build only (skip tests)
mvn clean compile

# Run tests for a single module (with dependencies)
mvn test -pl agentarts-sdk-core -am

# Run a specific test class
mvn test -pl agentarts-sdk-service -am -Dtest=RuntimeClientTest -Dsurefire.failIfNoSpecifiedTests=false
```

## Architecture

**14-module Maven project** using `${revision}` property + flatten-maven-plugin for version management. The BOM module (`agentarts-sdk-bom`) is standalone (no parent) to avoid circular dependency.

### Dual-Plane Pattern

Most service clients follow a **dual-plane architecture**: control plane (AK/SK signed) for resource management + data plane (API Key or Bearer token) for data operations. Each plane gets its own `BaseHttpClient` instance, initialized lazily with `synchronized`.

- `MemoryClient` — control: spaces/API keys; data: sessions/messages/memories
- `CodeInterpreterClient` — control: interpreter CRUD; data: execute/upload/download
- `RuntimeClient` — control: agent/endpoint CRUD; data: invoke/exec/upload/download/sessions
- `MCPGatewayClient` — single plane (always AK/SK)

### Signing

`V11Signer` implements V11-HMAC-SHA256 with HKDF key derivation (AK as salt, SK as IKM — reversed from typical HKDF). `BaseHttpClient` supports both `SignMode.V11_HMAC_SHA256` and `SignMode.SDK_HMAC_SHA256`.

### Message Serialization

Memory messages (`TextMessage`, `ToolCallMessage`, `ToolResultMessage`) serialize to OpenAPI "parts" format via `toDict()`:
```json
{"role": "user", "parts": [{"type": "text", "text": "Hello"}]}
```

### Shared Utilities

- `JsonUtils.MAPPER` — singleton `ObjectMapper` (use this, never create new instances)
- `JsonUtils.isNotBlank()` / `isBlank()` — null-safe string checks (use everywhere instead of `!= null && !isEmpty()`)
- `Constants` — all env vars, header names, endpoint constructors
- `E2EConfig` / `E2EResourceRegistry` / `E2EHelpers` — e2e test infrastructure

### Runtime Server

`AgentArtsRuntimeApp` (Vert.x) exposes `POST /invocations` (JSON + SSE), `GET /ping`, `WS /ws`. Handler fields are `volatile`. `AgentArtsRuntimeContext` uses ThreadLocal with mandatory `clear()` in `finally` blocks.

## Testing

**292 tests** across three tiers:

- **Unit tests** (223) — per-module, no cloud credentials needed
- **E2E read-only** — list/get operations, no writes
- **E2E lifecycle** (`AGENTARTS_TEST_ALLOW_CREATE=1`) — create→get→update→delete with LIFO cleanup via `E2EResourceRegistry`
- **E2E billable** (`AGENTARTS_TEST_RUN_BILLABLE=1`) — code interpreter sessions, runtime invoke (costs real money)

Tests that require cloud credentials use `assumeTrue()` at `@BeforeAll` level, which silently skips the entire class when conditions aren't met.

## Documentation

- Chinese user guides: `docs/cn/sdk_user_guide/` (6 files) and `docs/cn/toolkit_user_guide/` (8 files)
- Example projects: `agentarts-sdk-examples/src/main/java/com/huaweicloud/agentarts/examples/`
- Python SDK pinned commit: `2f64a4b` (branch `feature/test`) — see README bottom for sync instructions

## Conventions

- All Javadoc and comments must use **functional descriptions** — never reference Python, "mirrors", "ported from", or migration language
- Python SDK Reference section is at the **bottom** of README (before License)
- `plan.md` has been deleted — do not recreate it
