package com.huaweicloud.agentarts.examples.tools;

import com.huaweicloud.agentarts.sdk.core.PingStatus;
import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;
import com.huaweicloud.agentarts.sdk.tools.CodeInterpreterClient;
import com.huaweicloud.agentarts.sdk.tools.CodeSession;

import java.util.Map;

/**
 * Code Interpreter 工具示例 — 展示如何使用代码解释器沙箱执行代码。
 *
 * <p>演示功能：</p>
 * <ul>
 *   <li>创建代码解释器沙箱</li>
 *   <li>在沙箱中执行 Python 代码</li>
 *   <li>执行 shell 命令</li>
 *   <li>上传和下载文件</li>
 *   <li>使用 {@link CodeSession} 自动管理会话生命周期</li>
 * </ul>
 *
 * <p>环境变量：</p>
 * <ul>
 *   <li>{@code HUAWEICLOUD_SDK_REGION} — 华为云区域</li>
 *   <li>{@code HUAWEICLOUD_SDK_CODE_INTERPRETER_API_KEY} — 代码解释器 API Key</li>
 *   <li>{@code AGENTARTS_CODE_INTERPRETER_NAME} — 预创建的代码解释器名称</li>
 * </ul>
 *
 * <h3>运行：</h3>
 * <pre>
 * export HUAWEICLOUD_SDK_CODE_INTERPRETER_API_KEY=your-api-key
 * export AGENTARTS_CODE_INTERPRETER_NAME=your-ci-name
 * mvn compile exec:java -pl agentarts-sdk-examples \
 *   -Dexec.mainClass="com.huaweicloud.agentarts.examples.tools.CodeInterpreterExample"
 * </pre>
 *
 * <h3>测试：</h3>
 * <pre>
 * # 执行代码
 * curl -X POST http://localhost:8080/invocations \
 *   -H "Content-Type: application/json" \
 *   -d '{"code": "print(1+1)", "language": "python"}'
 *
 * # 执行命令
 * curl -X POST http://localhost:8080/invocations \
 *   -H "Content-Type: application/json" \
 *   -d '{"command": "echo hello"}'
 * </pre>
 */
public class CodeInterpreterExample {

    private static final AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        String region = System.getenv().getOrDefault("HUAWEICLOUD_SDK_REGION", "cn-southwest-2");
        String ciName = System.getenv().getOrDefault("AGENTARTS_CODE_INTERPRETER_NAME", "");

        app.setEntrypoint((Map<String, Object> payload) -> {
            String code = (String) payload.get("code");
            String command = (String) payload.get("command");
            String language = (String) payload.getOrDefault("language", "python");

            if (ciName.isEmpty()) {
                return Map.of("error", "AGENTARTS_CODE_INTERPRETER_NAME is required");
            }

            try (CodeSession session = CodeSession.start(region, ciName, "example-session")) {
                CodeInterpreterClient client = session.getClient();

                if (code != null && !code.isEmpty()) {
                    // 执行代码
                    Map<String, Object> result = client.executeCode(code, language, false);
                    return Map.of(
                            "type", "code_execution",
                            "result", result != null ? result : "No output",
                            "session_id", session.getSessionId());
                } else if (command != null && !command.isEmpty()) {
                    // 执行命令
                    Map<String, Object> result = client.executeCommand(command);
                    return Map.of(
                            "type", "command_execution",
                            "result", result != null ? result : "No output",
                            "session_id", session.getSessionId());
                } else {
                    return Map.of("error", "Either 'code' or 'command' is required");
                }
            }
        });

        app.setPingHandler(() -> PingStatus.HEALTHY);

        System.out.println("Starting Code Interpreter Example...");
        System.out.println("  POST /invocations - Execute code or command");
        System.out.println("  GET  /ping         - Health check");
        app.run(8080);
    }
}
