# agentarts mcp-gateway — MCP 网关管理

`mcp-gateway` 命令提供 MCP 网关和目标的 CRUD 管理功能。

## 前置条件

设置 AK/SK 环境变量：

```bash
export HUAWEICLOUD_SDK_AK="your-access-key"
export HUAWEICLOUD_SDK_SK="your-secret-key"
```

## 网关命令

### create-mcp-gateway — 创建网关

```bash
# 基本创建
agentarts mcp-gateway create-mcp-gateway --name my-gateway --description "测试网关"

# 完整配置
agentarts mcp-gateway create-mcp-gateway \
  --name my-gateway \
  --description "生产网关" \
  --protocol-type mcp \
  --authorizer-type iam \
  --agency-name AgentArtsCoreGateway
```

### update-mcp-gateway — 更新网关

`gatewayId` 为位置参数（不是 `--gateway-id` 选项）：

```bash
agentarts mcp-gateway update-mcp-gateway gw-123 --description "更新后的描述"
```

### delete-mcp-gateway — 删除网关

```bash
agentarts mcp-gateway delete-mcp-gateway gw-123
```

### get-mcp-gateway — 获取网关

```bash
agentarts mcp-gateway get-mcp-gateway gw-123
```

### list-mcp-gateways — 列出网关

```bash
# 列出全部
agentarts mcp-gateway list-mcp-gateways

# 按名称过滤
agentarts mcp-gateway list-mcp-gateways --name my-gateway

# 分页
agentarts mcp-gateway list-mcp-gateways --limit 10 --offset 0
```

## 目标命令

### create-mcp-gateway-target — 创建目标

`gatewayId` 为位置参数；`--target-configuration`（JSON）为必填：

```bash
agentarts mcp-gateway create-mcp-gateway-target gw-123 \
  --name my-target \
  --description "测试目标" \
  --target-configuration '{"mcp_server":{"endpoint":"https://example.com/mcp","server_type":"sse"}}'
```

### update-mcp-gateway-target — 更新目标

`gatewayId` 与 `targetId` 均为位置参数（前两个位置参数）：

```bash
agentarts mcp-gateway update-mcp-gateway-target gw-123 tgt-456 \
  --name new-name \
  --description "新描述"
```

### delete-mcp-gateway-target — 删除目标

```bash
agentarts mcp-gateway delete-mcp-gateway-target gw-123 tgt-456
```

### get-mcp-gateway-target — 获取目标

```bash
agentarts mcp-gateway get-mcp-gateway-target gw-123 tgt-456
```

### list-mcp-gateway-targets — 列出目标

`gatewayId` 为位置参数：

```bash
# 列出网关下所有目标
agentarts mcp-gateway list-mcp-gateway-targets gw-123

# 分页
agentarts mcp-gateway list-mcp-gateway-targets gw-123 --limit 10 --offset 0
```

## 最佳实践

1. **先创建网关，再创建目标** — 目标必须关联到一个网关
2. **使用有意义的名称** — 便于后续查找和管理
3. **定期清理不用的网关和目标** — 避免资源浪费

## 常见问题

### 403 错误

如果收到 `Operation not supported. The tenant has not enabled the service.` 错误，表示租户尚未开通 MCP Gateway 服务。请在华为云控制台开通后再试。
