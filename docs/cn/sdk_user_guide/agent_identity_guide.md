# Agent Identity（代理身份）SDK 使用指南

AgentArts Agent Identity SDK 提供工作负载身份管理、凭证提供者管理和访问令牌获取能力。

> 生产要求：凭证获取失败会向调用方抛出异常，不会以空字符串继续执行；日志不得输出 API Key、OAuth2 token、STS secret 或 workload access token。

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

```powershell
$env:HUAWEICLOUD_SDK_AK = "your-access-key"
$env:HUAWEICLOUD_SDK_SK = "your-secret-key"
$env:HUAWEICLOUD_SDK_REGION = "cn-southwest-2"
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

// 获取资源 API Key 字符串
String apiKey = client.getResourceApiKeyValue("my-api-key-provider", token);
```

### 使用注解（装饰器模式）

Java SDK 提供与 Python `@require_*` 等价的注解：

注解只有通过 `AuthInterceptor` 创建的 JDK 动态代理调用时才会生效；直接调用实现类不会拦截。注解应声明在接口方法上：

```java
import com.huaweicloud.agentarts.sdk.core.annotation.RequireApiKey;
import com.huaweicloud.agentarts.sdk.identity.auth.AuthInterceptor;

interface ProtectedService {
    @RequireApiKey(providerName = "my-api-key-provider", into = "apiKey")
    String call(String payload, String apiKey);
}

ProtectedService target = (payload, apiKey) -> {
    // 使用注入的 apiKey 调用目标服务；不得记录该值
    return payload;
};
ProtectedService service = AuthInterceptor.wrap(
        target, ProtectedService.class, new IdentityClient());
String result = service.call("request", null);
```

`@RequireAccessToken` 同时支持 `M2M` 与 `USER_FEDERATION`，`@RequireStsToken` 注入 `GetResourceStsTokenResponseBodyCredentials`。一个方法最多声明一个 `@Require*` 注解；代理会优先按 `into` 参数名定位，消费方建议使用 `javac -parameters`，否则必须保证凭证类型在参数中唯一。

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
| `createOauth2CredentialProvider(name, vendor, clientId, clientSecret)` | 创建 OAuth2 凭证提供者（vendor 为 `CredentialProviderVendor` 枚举） |
| `createStsCredentialProvider(providerName, agencyUrn)` | 创建 STS 凭证提供者（agencyUrn 形如 `iam::<agencyName>`） |

### 资源凭证获取

| 方法 | 说明 |
|------|------|
| `getResourceApiKey(providerName, workloadAccessToken)` | 获取资源 API Key |
| `getResourceApiKeyValue(providerName, workloadAccessToken)` | 获取资源 API Key 字符串（注解注入使用） |
| `getResourceOauth2Token(providerName, workloadAccessToken)` | 获取资源 OAuth2 令牌 |
| `getResourceStsToken(providerName, workloadAccessToken, agencySessionName)` | 获取资源 STS 令牌 |
| `completeResourceTokenAuth(sessionUri, userIdentifier)` | 完成资源令牌授权并绑定用户标识 |

### 本地身份管理

| 方法 | 说明 |
|------|------|
| `ensureLocalAuthToken(workloadName)` | 确保本地 `.agent_identity.json` 中有有效的令牌 |

`.agent_identity.json` 采用临时文件 + 原子替换写入，并在支持 POSIX 权限的平台限制为 owner 读写。它仍属于敏感本地状态：不要提交、复制到镜像或在日志中打印。

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

真实鉴权验收必须检查目标测试实际执行而非 assumption 跳过：AK/SK 只读、API Key、STS 和 OAuth2 是独立路径，其中任一路径成功都不能替代其他路径的验证。
