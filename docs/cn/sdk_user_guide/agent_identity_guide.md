# Agent Identity（代理身份）SDK 使用指南

AgentArts Agent Identity SDK 提供工作负载身份管理、凭证提供者管理和访问令牌获取能力。

## 概述

Agent Identity SDK 支持三种凭证获取方式：

- **OAuth2** — 三方授权流程（USER_FEDERATION / M2M）
- **API Key** — 简单的 API Key 认证
- **STS Token** — 临时安全令牌

## 身份验证

```bash
export HUAWEICLOUD_SDK_AK="your-access-key"
export HUAWEICLOUD_SDK_SK="your-secret-key"
export HUAWEICLOUD_SDK_REGION="cn-southwest-2"
```

## 快速入门

### 使用 IdentityClient

```java
import com.huaweicloud.agentarts.sdk.identity.IdentityClient;

IdentityClient client = new IdentityClient("cn-southwest-2");

// 创建工作负载身份
client.createWorkloadIdentity("my-agent");

// 获取工作负载身份
var identity = client.getWorkloadIdentity("my-agent");

// 列出工作负载身份
var identities = client.listWorkloadIdentities();

// 创建工作负载访问令牌
String token = client.createWorkloadAccessToken("my-agent");

// 创建 API Key 凭证提供者
client.createApiKeyCredentialProvider("my-api-key-provider", "your-api-key-value");

// 获取资源 API Key
var apiKey = client.getResourceApiKey("my-api-key-provider", token);
```

### 使用注解（装饰器模式）

Java SDK 提供与 Python `@require_*` 等价的注解：

```java
import com.huaweicloud.agentarts.sdk.core.annotation.RequireAccessToken;
import com.huaweicloud.agentarts.sdk.core.annotation.RequireApiKey;
import com.huaweicloud.agentarts.sdk.core.annotation.RequireStsToken;

// OAuth2 USER_FEDERATION 流程
@RequireAccessToken(providerName = "my-oauth2-provider", authFlow = "USER_FEDERATION")
public void handleOAuth2(String accessToken) {
    // accessToken 自动注入
}

// OAuth2 M2M 流程
@RequireAccessToken(providerName = "my-m2m-provider", authFlow = "M2M")
public void handleM2M(String accessToken) {
    // accessToken 自动注入
}

// API Key
@RequireApiKey(providerName = "my-api-key-provider")
public void handleApiKey(String apiKey) {
    // apiKey 自动注入
}

// STS Token
@RequireStsToken(providerName = "my-sts-provider", agencySessionName = "my-session")
public void handleSts(StsCredentials credentials) {
    // credentials.getAccessKeyId(), credentials.getSecretAccessKey(), credentials.getSecurityToken()
}
```

## API 参考

### IdentityClient 构造函数

```java
IdentityClient client = new IdentityClient(region);
IdentityClient client = new IdentityClient(region, ignoreSslVerification);
IdentityClient client = new IdentityClient();  // 使用默认区域
```

### 工作负载身份管理

| 方法 | 说明 |
|------|------|
| `createWorkloadIdentity(name)` | 创建工作负载身份 |
| `getWorkloadIdentity(name)` | 获取工作负载身份 |
| `listWorkloadIdentities()` | 列出所有工作负载身份 |
| `deleteWorkloadIdentity(name)` | 删除工作负载身份 |

### 访问令牌

| 方法 | 说明 |
|------|------|
| `createWorkloadAccessToken(workloadName)` | 创建工作负载访问令牌 |
| `createWorkloadAccessTokenForUserId(workloadName, userId)` | 为指定用户创建令牌 |
| `createWorkloadAccessTokenForJwt(workloadName, userToken)` | 使用 JWT 创建令牌 |

### 凭证提供者管理

| 方法 | 说明 |
|------|------|
| `createApiKeyCredentialProvider(providerName, apiKey)` | 创建 API Key 凭证提供者 |
| `createOauth2CredentialProvider(providerName)` | 创建 OAuth2 凭证提供者 |
| `createStsCredentialProvider(providerName)` | 创建 STS 凭证提供者 |

### 资源凭证获取

| 方法 | 说明 |
|------|------|
| `getResourceApiKey(providerName, workloadAccessToken)` | 获取资源 API Key |
| `getResourceOauth2Token(providerName, workloadAccessToken)` | 获取资源 OAuth2 令牌 |
| `getResourceStsToken(providerName, workloadAccessToken, agencySessionName)` | 获取资源 STS 令牌 |
| `completeResourceTokenAuth(sessionUri)` | 完成资源令牌授权 |

### 本地身份管理

| 方法 | 说明 |
|------|------|
| `ensureLocalAuthToken(workloadName)` | 确保本地 `.agent_identity.json` 中有有效的令牌 |

## AgentArtsRuntimeContext

在运行时上下文中管理身份信息：

```java
import com.huaweicloud.agentarts.sdk.runtime.context.AgentArtsRuntimeContext;

// 设置和获取用户 ID
AgentArtsRuntimeContext.setUserId("user-123");
String userId = AgentArtsRuntimeContext.getUserId();

// OAuth2 相关
AgentArtsRuntimeContext.setOAuth2CallbackUrl("https://callback.example.com");
AgentArtsRuntimeContext.setOAuth2CustomState("custom-state");

// 清理（必须在 finally 块中调用）
AgentArtsRuntimeContext.clear();
```

## 完整示例

参见 `agentarts-sdk-examples` 模块和 E2E 测试中的 `AuthDecoratorsTest`。
