package com.huaweicloud.agentarts.examples.memory;

import com.huaweicloud.agentarts.sdk.core.PingStatus;
import com.huaweicloud.agentarts.sdk.memory.MemoryClient;
import com.huaweicloud.agentarts.sdk.memory.model.*;
import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Memory Service 示例 — 展示如何使用 AgentArts Memory Service 存储对话历史。
 *
 * <p>演示功能：</p>
 * <ul>
 *   <li>创建对话会话</li>
 *   <li>存储消息到 Memory Service</li>
 *   <li>检索对话历史</li>
 * </ul>
 *
 * <p>环境变量：</p>
 * <ul>
 *   <li>{@code HUAWEICLOUD_SDK_MEMORY_API_KEY} — Memory Service API Key</li>
 *   <li>{@code HUAWEICLOUD_SDK_REGION} — 华为云区域（默认 cn-southwest-2）</li>
 *   <li>{@code AGENTARTS_MEMORY_SPACE_ID} — Memory Space ID</li>
 * </ul>
 *
 * <h3>运行：</h3>
 * <pre>
 * export HUAWEICLOUD_SDK_MEMORY_API_KEY=your-api-key
 * export AGENTARTS_MEMORY_SPACE_ID=your-space-id
 * mvn compile exec:java -pl agentarts-sdk-examples \
 *   -Dexec.mainClass="com.huaweicloud.agentarts.examples.memory.MemoryUsageExample"
 * </pre>
 *
 * <h3>测试：</h3>
 * <pre>
 * # 发送消息（自动创建 session）
 * curl -X POST http://localhost:8080/invocations \
 *   -H "Content-Type: application/json" \
 *   -d '{"message": "Hello!"}'
 *
 * # 使用相同 session 继续对话
 * curl -X POST http://localhost:8080/invocations \
 *   -H "Content-Type: application/json" \
 *   -d '{"message": "What did I say before?", "session_id": "xxx"}'
 * </pre>
 */
public class MemoryUsageExample {

    private static final AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
    private static final MemoryClient memoryClient = new MemoryClient(
            System.getenv("HUAWEICLOUD_SDK_REGION"),
            System.getenv("HUAWEICLOUD_SDK_MEMORY_API_KEY"));

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        String spaceId = System.getenv("AGENTARTS_MEMORY_SPACE_ID");

        app.setEntrypoint((Map<String, Object> payload) -> {
            String message = (String) payload.getOrDefault("message", "");
            String sessionId = (String) payload.get("session_id");
            String effectiveSpaceId = (String) payload.getOrDefault("space_id", spaceId);

            if (effectiveSpaceId == null || effectiveSpaceId.isEmpty()) {
                return Map.of(
                        "error", "space_id is required. Set AGENTARTS_MEMORY_SPACE_ID env var or pass in payload.",
                        "session_id", sessionId != null ? sessionId : "error",
                        "history", List.of());
            }

            // 创建或复用 session
            String effectiveSessionId = sessionId;
            if (effectiveSessionId == null) {
                SessionInfo session = memoryClient.createMemorySession(effectiveSpaceId);
                effectiveSessionId = session.getId();
            }

            // 存储用户消息
            TextMessage userMsg = new TextMessage("user", message);
            memoryClient.addMessages(effectiveSpaceId, effectiveSessionId, List.of(userMsg));

            // 获取对话历史
            List<MessageInfo> history = memoryClient.getLastKMessages(
                    effectiveSessionId, 10, effectiveSpaceId);

            String responseText = "You said: " + message + ". I remember our conversation!";

            // 存储 assistant 回复
            TextMessage assistantMsg = new TextMessage("assistant", responseText);
            memoryClient.addMessages(effectiveSpaceId, effectiveSessionId, List.of(assistantMsg));

            // 构建历史记录
            List<Map<String, String>> historyList = new ArrayList<>();
            for (MessageInfo msg : history) {
                Map<String, String> entry = new HashMap<>();
                entry.put("role", msg.getRole());
                if (msg.getParts() != null && !msg.getParts().isEmpty()) {
                    Map<String, Object> part = msg.getParts().get(0);
                    Object content = part.getOrDefault("text", part.getOrDefault("content", ""));
                    entry.put("content", String.valueOf(content));
                }
                historyList.add(entry);
            }

            return Map.of(
                    "response", responseText,
                    "session_id", effectiveSessionId,
                    "history", historyList);
        });

        app.setPingHandler(() -> PingStatus.HEALTHY);

        System.out.println("Starting Agent with Memory Example...");
        System.out.println("  POST /invocations - Invoke the agent");
        System.out.println("  GET  /ping         - Health check");
        app.run(8080);
    }
}
