# Code Interpreter（代码解释器）SDK 使用指南

AgentArts Code Interpreter SDK 提供代码沙箱执行能力，支持代码执行、命令执行、文件上传/下载和包安装。

## 概述

Code Interpreter SDK 采用**双平面架构**：

- **控制平面**（AK/SK 签名）— 创建/管理代码解释器实例
- **数据平面**（API Key 或 IAM 认证）— 在沙箱中执行代码和操作文件

## 身份验证

### AK/SK 认证（控制平面）

```bash
export HUAWEICLOUD_SDK_AK="your-access-key"
export HUAWEICLOUD_SDK_SK="your-secret-key"
```

### API Key 认证（数据平面）

```bash
export HUAWEICLOUD_SDK_CODE_INTERPRETER_API_KEY="your-ci-api-key"
```

### 数据面端点（可选）

```bash
export AGENTARTS_CODEINTERPRETER_DATA_ENDPOINT="https://your-ci-endpoint.com"
```

## 快速入门

### 使用 CodeSession 上下文管理器（推荐）

`CodeSession` 自动管理会话生命周期，退出时自动关闭会话：

```java
import com.huaweicloud.agentarts.sdk.tools.CodeSession;
import com.huaweicloud.agentarts.sdk.tools.CodeInterpreterClient;

try (CodeSession session = CodeSession.start("cn-southwest-2", "my-ci-name", "my-session")) {
    CodeInterpreterClient client = session.getClient();

    // 执行代码
    Map<String, Object> result = client.executeCode("print(1+1)");

    // 执行命令
    Map<String, Object> cmdResult = client.executeCommand("echo hello");

    // 上传文件
    client.uploadFile("/tmp/test.txt", "file content", "description");

    // 下载文件
    Object content = client.downloadFile("/tmp/test.txt");
}
// 退出 try 块时自动调用 stopSession()
```

### 手动管理会话

```java
import com.huaweicloud.agentarts.sdk.tools.CodeInterpreterClient;

CodeInterpreterClient client = new CodeInterpreterClient("cn-southwest-2");

// 启动会话
String sessionId = client.startSession("my-ci-name", "my-session");

// 执行代码
Map<String, Object> result = client.executeCode("print('Hello World')", "python", false);

// 执行命令
Map<String, Object> cmdResult = client.executeCommand("ls -la");

// 获取会话状态
CodeInterpreterSessionInfo session = client.getSession("my-ci-name", sessionId);

// 关闭会话
client.stopSession();
client.close();
```

## API 参考

### 构造函数

```java
// 使用默认认证类型（API_KEY）
CodeInterpreterClient client = new CodeInterpreterClient(region);

// 指定数据端点
CodeInterpreterClient client = new CodeInterpreterClient(region, dataEndpoint);

// 完整参数
CodeInterpreterClient client = new CodeInterpreterClient(region, dataEndpoint, authType, verifySsl);
```

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `region` | 华为云区域 | `Constants.getRegion()` |
| `dataEndpoint` | 数据面端点 URL | 自动检测 |
| `authType` | 认证类型：`"API_KEY"` 或 `"IAM"` | `"API_KEY"` |
| `verifySsl` | 是否验证 SSL | `true` |

### 控制平面：代码解释器管理

#### createCodeInterpreter — 创建代码解释器

```java
CodeInterpreterInfo ci = client.createCodeInterpreter("my-ci", "描述");
// ci.getId() → 代码解释器 ID
```

#### listCodeInterpreters — 列出代码解释器

```java
CodeInterpreterListResponse result = client.listCodeInterpreters(null, 10, 0);
// name, limit, offset
```

#### getCodeInterpreter — 获取代码解释器

```java
CodeInterpreterInfo ci = client.getCodeInterpreter("ci-id-123");
```

#### updateCodeInterpreter — 更新代码解释器

```java
Map<String, Object> updates = Map.of("tags", List.of(Map.of("key", "env", "value", "test")));
CodeInterpreterInfo updated = client.updateCodeInterpreter("ci-id-123", updates);
```

#### deleteCodeInterpreter — 删除代码解释器

```java
client.deleteCodeInterpreter("ci-id-123");
```

### 数据平面：会话管理

#### startSession — 启动会话

```java
String sessionId = client.startSession("ci-name", "session-name", 900);
// codeInterpreterName, sessionName, sessionTimeout(秒)
```

#### getSession — 获取会话状态

```java
CodeInterpreterSessionInfo session = client.getSession("ci-name", "session-id");
```

#### stopSession — 关闭会话

```java
boolean success = client.stopSession();
```

### 数据平面：代码执行

#### executeCode — 执行代码

```java
Map<String, Object> result = client.executeCode(
    "print(1+1)",       // code: 代码字符串
    "python",           // language: 编程语言
    false               // clearContext: 是否清除上下文
);
```

#### executeCommand — 执行 Shell 命令

```java
Map<String, Object> result = client.executeCommand("echo hello");
```

#### invoke — 通用调用

```java
Map<String, Object> result = client.invoke("execute_code", Map.of(
    "code", "print(1+1)",
    "language", "python",
    "clear_context", false
));
```

### 数据平面：文件操作

#### uploadFile — 上传单个文件

```java
Map<String, Object> result = client.uploadFile(
    "/home/user/test.txt",  // path: 沙箱内路径
    "file content",          // content: 文件内容
    "description"            // description: 文件描述
);
```

#### uploadFiles — 上传多个文件

```java
List<Map<String, String>> files = List.of(
    Map.of("path", "/home/user/a.txt", "content", "content a"),
    Map.of("path", "/home/user/b.txt", "content", "content b")
);
Map<String, Object> result = client.uploadFiles(files);
```

#### downloadFile — 下载单个文件

```java
Object content = client.downloadFile("/home/user/test.txt");
```

#### downloadFiles — 下载多个文件

```java
Map<String, Object> result = client.downloadFiles(List.of("/home/user/a.txt", "/home/user/b.txt"));
```

### 数据平面：环境管理

#### installPackages — 安装包

```java
Map<String, Object> result = client.installPackages(List.of("numpy", "pandas"), false);
// packages, upgrade
```

#### clearContext — 清除上下文

```java
Map<String, Object> result = client.clearContext();
```

## CodeSession 上下文管理器

```java
// 基本用法
try (CodeSession session = CodeSession.start(region, ciName, sessionName)) {
    CodeInterpreterClient client = session.getClient();
    // 使用 client 执行操作...
}
// 自动调用 stopSession()

// 使用完整参数
try (CodeSession session = CodeSession.start(region, ciName, sessionName, authType, verifySsl)) {
    // ...
}
```

## 完整示例

参见 `agentarts-sdk-examples` 模块中的 `CodeInterpreterExample`：

```bash
export HUAWEICLOUD_SDK_CODE_INTERPRETER_API_KEY=your-api-key
export AGENTARTS_CODE_INTERPRETER_NAME=your-ci-name
mvn compile exec:java -pl agentarts-sdk-examples \
  -Dexec.mainClass="com.huaweicloud.agentarts.examples.tools.CodeInterpreterExample"
```
