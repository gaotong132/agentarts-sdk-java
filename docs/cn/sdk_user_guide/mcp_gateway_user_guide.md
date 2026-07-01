# MCP Gateway SDK 使用指南

AgentArts MCP Gateway SDK 提供 MCP 网关和目标的管理能力，始终使用 AK/SK 签名认证。

## 概述

MCP Gateway 是 Model Context Protocol 的服务端网关，用于管理 MCP 工具的路由和调用。SDK 提供完整的网关和目标 CRUD 操作。

## 身份验证

MCP Gateway 始终使用 AK/SK 签名认证：

```bash
export HUAWEICLOUD_SDK_AK="your-access-key"
export HUAWEICLOUD_SDK_SK="your-secret-key"
```

## 快速入门

```java
import com.huaweicloud.agentarts.sdk.mcpgateway.MCPGatewayClient;
import com.huaweicloud.agentarts.sdk.service.http.RequestResult;

try (MCPGatewayClient client = new MCPGatewayClient()) {
    // 创建网关
    RequestResult gw = client.createMcpGateway("my-gateway", "测试网关");

    // 创建目标
    RequestResult target = client.createMcpGatewayTarget(gatewayId, "my-target", "测试目标");

    // 列出网关
    RequestResult list = client.listMcpGateways();
}
```

## API 参考

### 构造函数

```java
// 默认 SSL 验证
MCPGatewayClient client = new MCPGatewayClient();

// 禁用 SSL 验证
MCPGatewayClient client = new MCPGatewayClient(false);
```

### 网关管理

#### createMcpGateway — 创建网关

```java
RequestResult result = client.createMcpGateway(
    "my-gateway",     // name: 网关名称
    "描述文本",        // description: 可选描述
    "mcp",            // protocolType: 协议类型（默认 "mcp"）
    "iam",            // authorizerType: 认证类型（默认 "iam"）
    null              // agencyName: IAM 代理名称（可选）
);
```

**返回**：`RequestResult`（success, data, error, statusCode）

#### updateMcpGateway — 更新网关

```java
RequestResult result = client.updateMcpGateway("gateway-id", "新描述");
```

#### deleteMcpGateway — 删除网关

```java
RequestResult result = client.deleteMcpGateway("gateway-id");
```

#### getMcpGateway — 获取网关

```java
RequestResult result = client.getMcpGateway("gateway-id");
if (result.isSuccess()) {
    Map<String, Object> data = (Map<String, Object>) result.getData();
}
```

#### listMcpGateways — 列出网关

```java
RequestResult result = client.listMcpGateways(
    null,  // name: 按名称过滤（可选）
    10,    // limit: 分页大小
    0      // offset: 偏移量
);
```

### 目标管理

#### createMcpGatewayTarget — 创建目标

```java
RequestResult result = client.createMcpGatewayTarget(
    "gateway-id",          // gatewayId
    "my-target",           // name
    "目标描述",             // description
    null,                  // targetConfiguration（可选）
    null                   // credentialProviderConfiguration（可选）
);
```

#### updateMcpGatewayTarget — 更新目标

```java
RequestResult result = client.updateMcpGatewayTarget(
    "gateway-id", "target-id",
    "new-name", "new-description",
    null, null
);
```

#### deleteMcpGatewayTarget — 删除目标

```java
RequestResult result = client.deleteMcpGatewayTarget("gateway-id", "target-id");
```

#### getMcpGatewayTarget — 获取目标

```java
RequestResult result = client.getMcpGatewayTarget("gateway-id", "target-id");
```

#### listMcpGatewayTargets — 列出目标

```java
RequestResult result = client.listMcpGatewayTargets("gateway-id", 10, 0);
// gatewayId, limit, offset
```

## RequestResult 返回类型

所有 MCP Gateway API 方法返回 `RequestResult`：

| 属性 | 类型 | 说明 |
|------|------|------|
| `isSuccess()` | boolean | 请求是否成功（2xx 状态码） |
| `getStatusCode()` | int | HTTP 状态码 |
| `getData()` | Object | 响应数据（JSON 解析后的 Map 或 JsonNode） |
| `getError()` | String | 错误信息（失败时） |
| `getHeaders()` | Map | 响应头 |

## 错误处理

```java
RequestResult result = client.createMcpGateway("my-gw", "desc");
if (!result.isSuccess()) {
    System.err.println("Error " + result.getStatusCode() + ": " + result.getError());
} else {
    Map<String, Object> data = (Map<String, Object>) result.getData();
    String gatewayId = (String) data.get("id");
}
```

## 最佳实践

1. **使用 try-with-resources** 确保客户端资源正确释放
2. **检查 `isSuccess()`** 在访问 `getData()` 之前
3. **处理 403 错误** — 可能表示租户未开通 MCP Gateway 服务
