# 环境变量配置指南

本文档详细说明 AgentArts Java SDK 支持的所有环境变量，包括用途、默认值和优先级规则。

## 华为云认证

### AK/SK 认证（必需）

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `HUAWEICLOUD_SDK_AK` | 华为云 Access Key ID | `AK_xxxxxxxxxxxxxxxx` |
| `HUAWEICLOUD_SDK_SK` | 华为云 Secret Access Key | `SK_xxxxxxxxxxxxxxxx` |

```bash
# Linux / macOS
export HUAWEICLOUD_SDK_AK="your-access-key"
export HUAWEICLOUD_SDK_SK="your-secret-key"
```

```powershell
# Windows PowerShell
$env:HUAWEICLOUD_SDK_AK = "your-access-key"
$env:HUAWEICLOUD_SDK_SK = "your-secret-key"
```

### STS 安全令牌（可选）

用于临时凭证场景，与 AK/SK 配合使用：

| 变量名 | 说明 |
|--------|------|
| `HUAWEICLOUD_SDK_SECURITY_TOKEN` | STS 安全令牌 |

```bash
export HUAWEICLOUD_SDK_SECURITY_TOKEN="your-security-token"
```

```powershell
$env:HUAWEICLOUD_SDK_SECURITY_TOKEN = "your-security-token"
```

## 区域配置

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `HUAWEICLOUD_SDK_REGION` | 华为云区域 | `cn-southwest-2` |
| `HUAWEICLOUD_REGION` | 备用区域变量 | — |
| `OS_REGION_NAME` | OpenStack 兼容区域 | — |

**优先级链**：`HUAWEICLOUD_SDK_REGION` → `HUAWEICLOUD_REGION` → `OS_REGION_NAME` → `cn-southwest-2`

```bash
export HUAWEICLOUD_SDK_REGION="cn-southwest-2"
```

```powershell
$env:HUAWEICLOUD_SDK_REGION = "cn-southwest-2"
```

Java SDK 中通过 `Constants.getRegion()` 获取区域，自动按上述优先级解析。

## 项目 ID

| 变量名 | 说明 |
|--------|------|
| `HUAWEICLOUD_SDK_PROJECT_ID` | 华为云项目 ID |

```bash
export HUAWEICLOUD_SDK_PROJECT_ID="your-project-id"
```

```powershell
$env:HUAWEICLOUD_SDK_PROJECT_ID = "your-project-id"
```

## IDP 配置

| 变量名 | 说明 |
|--------|------|
| `HUAWEICLOUD_SDK_IDP_ID` | 身份提供商 ID |
| `HUAWEICLOUD_SDK_ID_TOKEN_FILE` | ID Token 文件路径 |

## AgentArts 服务端点

### 控制面端点

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `AGENTARTS_CONTROL_ENDPOINT` | 控制面端点 URL | `https://agentarts.{region}.myhuaweicloud.com` |

```bash
export AGENTARTS_CONTROL_ENDPOINT="https://agentarts.cn-southwest-2.myhuaweicloud.com"
```

```powershell
$env:AGENTARTS_CONTROL_ENDPOINT = "https://agentarts.cn-southwest-2.myhuaweicloud.com"
```

### 数据面端点

| 变量名 | 说明 |
|--------|------|
| `AGENTARTS_RUNTIME_DATA_ENDPOINT` | 运行时数据面端点 |
| `AGENTARTS_MEMORY_DATA_ENDPOINT` | Memory 数据面端点 |
| `AGENTARTS_CODEINTERPRETER_DATA_ENDPOINT` | 代码解释器数据面端点 |

```bash
export AGENTARTS_RUNTIME_DATA_ENDPOINT="https://your-runtime-endpoint.com"
export AGENTARTS_MEMORY_DATA_ENDPOINT="https://memory.cn-southwest-2.huaweicloud-agentarts.com"
export AGENTARTS_CODEINTERPRETER_DATA_ENDPOINT="https://your-ci-endpoint.com"
```

```powershell
$env:AGENTARTS_RUNTIME_DATA_ENDPOINT = "https://your-runtime-endpoint.com"
$env:AGENTARTS_MEMORY_DATA_ENDPOINT = "https://memory.cn-southwest-2.huaweicloud-agentarts.com"
$env:AGENTARTS_CODEINTERPRETER_DATA_ENDPOINT = "https://your-ci-endpoint.com"
```

## 华为云服务端点

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `HUAWEICLOUD_SDK_IAM_ENDPOINT` | IAM 服务端点 | `https://iam.{region}.myhuaweicloud.com` |
| `HUAWEICLOUD_SDK_SWR_ENDPOINT` | SWR 服务端点 | `https://swr-api.{region}.myhuaweicloud.com` |
| `HUAWEICLOUD_SDK_AGENTIDENTITY_ENDPOINT` | 代理身份服务端点 | `https://agent-identity.{region}.myhuaweicloud.com` |

## 运行时配置

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `AGENTARTS_LOG_LEVEL` | 日志级别（DEBUG/INFO/WARNING/ERROR） | `INFO` |
| `AGENTARTS_BIND_IP` | 运行时绑定 IP | `0.0.0.0` |

## 配置文件

### 基础配置（.env）

```bash
# .env 基础配置
HUAWEICLOUD_SDK_AK=your-access-key
HUAWEICLOUD_SDK_SK=your-secret-key
HUAWEICLOUD_SDK_REGION=cn-southwest-2
```

### 完整配置（.env）

```bash
# .env 完整配置
HUAWEICLOUD_SDK_AK=your-access-key
HUAWEICLOUD_SDK_SK=your-secret-key
HUAWEICLOUD_SDK_REGION=cn-southwest-2
HUAWEICLOUD_SDK_PROJECT_ID=your-project-id

AGENTARTS_CONTROL_ENDPOINT=https://agentarts.cn-southwest-2.myhuaweicloud.com
AGENTARTS_RUNTIME_DATA_ENDPOINT=https://your-runtime-endpoint.com
AGENTARTS_MEMORY_DATA_ENDPOINT=https://memory.cn-southwest-2.huaweicloud-agentarts.com
AGENTARTS_CODEINTERPRETER_DATA_ENDPOINT=https://your-ci-endpoint.com

AGENTARTS_LOG_LEVEL=DEBUG
AGENTARTS_BIND_IP=0.0.0.0
```

## Java SDK 常量对照表

| 环境变量 | Java 常量 | 访问方法 |
|----------|-----------|----------|
| `HUAWEICLOUD_SDK_AK` | `Constants.ENV_HUAWEICLOUD_SDK_AK` | `Constants.getAk()` |
| `HUAWEICLOUD_SDK_SK` | `Constants.ENV_HUAWEICLOUD_SDK_SK` | `Constants.getSk()` |
| `HUAWEICLOUD_SDK_REGION` | `Constants.ENV_HUAWEICLOUD_SDK_REGION` | `Constants.getRegion()` |
| `HUAWEICLOUD_SDK_SECURITY_TOKEN` | `Constants.ENV_HUAWEICLOUD_SDK_SECURITY_TOKEN` | `Constants.getSecurityToken()` |
| `AGENTARTS_CONTROL_ENDPOINT` | `Constants.ENV_AGENTARTS_CONTROL_ENDPOINT` | `Constants.getControlPlaneEndpoint()` |
| `AGENTARTS_RUNTIME_DATA_ENDPOINT` | `Constants.ENV_AGENTARTS_RUNTIME_DATA_ENDPOINT` | `Constants.getRuntimeDataPlaneEndpoint()` |
| `AGENTARTS_MEMORY_DATA_ENDPOINT` | `Constants.ENV_AGENTARTS_MEMORY_DATA_ENDPOINT` | `Constants.getMemoryEndpoint("data")` |
| `AGENTARTS_CODEINTERPRETER_DATA_ENDPOINT` | `Constants.ENV_AGENTARTS_CODEINTERPRETER_DATA_ENDPOINT` | `Constants.getCodeInterpreterDataPlaneEndpoint()` |

## 常见问题

### 环境变量不生效？

确保在启动 Java 应用之前已导出环境变量：

```bash
export HUAWEICLOUD_SDK_AK="your-ak"
export HUAWEICLOUD_SDK_SK="your-sk"
java -jar your-app.jar
```

```powershell
$env:HUAWEICLOUD_SDK_AK = "your-ak"
$env:HUAWEICLOUD_SDK_SK = "your-sk"
java -jar your-app.jar
```

或通过 JVM 参数传递：

```bash
java -DHUAWEICLOUD_SDK_AK=your-ak -DHUAWEICLOUD_SDK_SK=your-sk -jar your-app.jar
```

### 区域选择

AgentArts 默认使用 `cn-southwest-2` 区域。如需使用其他区域，请设置 `HUAWEICLOUD_SDK_REGION`。
