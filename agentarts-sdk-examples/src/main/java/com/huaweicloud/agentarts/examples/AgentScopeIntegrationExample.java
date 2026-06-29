package com.huaweicloud.agentarts.examples;

import com.huaweicloud.agentarts.sdk.core.PingStatus;
import com.huaweicloud.agentarts.sdk.integration.agentscope.message.MessageConverter;
import com.huaweicloud.agentarts.sdk.integration.agentscope.runtime.AgentscopeRuntimeHost;
import com.huaweicloud.agentarts.sdk.integration.agentscope.state.MemoryAgentStateStore;
import com.huaweicloud.agentarts.sdk.memory.MemoryClient;
import com.huaweicloud.agentarts.sdk.memory.model.TextMessage;
import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;
import com.huaweicloud.agentarts.sdk.runtime.context.RequestContext;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.state.State;

import java.util.Map;
import java.util.Optional;

/**
 * agentscope-java integration example: demonstrates bridging AgentArts Runtime
 * with agentscope's ReActAgent pattern.
 *
 * <p>Demonstrates:</p>
 * <ul>
 *   <li>{@link AgentscopeRuntimeHost} — bridges RequestContext → RuntimeContext</li>
 *   <li>{@link MemoryAgentStateStore} — persists agent state via AgentArts Memory</li>
 *   <li>{@link MessageConverter} — converts between AgentArts and agentscope message types</li>
 * </ul>
 *
 * <p><b>Note:</b> This example requires a valid Memory API key and space ID.
 * Set {@code AGENTARTS_MEMORY_API_KEY} and {@code AGENTARTS_MEMORY_SPACE_ID}
 * environment variables, or provide them via constructor arguments.</p>
 *
 * <h3>Run:</h3>
 * <pre>
 * export AGENTARTS_MEMORY_API_KEY=your-api-key
 * export AGENTARTS_MEMORY_SPACE_ID=your-space-id
 * mvn compile exec:java -pl agentarts-sdk-examples \
 *   -Dexec.mainClass="com.huaweicloud.agentarts.examples.AgentScopeIntegrationExample"
 * </pre>
 */
public class AgentScopeIntegrationExample {

    public static void main(String[] args) {
        String region = System.getenv().getOrDefault("HUAWEICLOUD_SDK_REGION", "cn-southwest-2");
        String apiKey = System.getenv().getOrDefault("AGENTARTS_MEMORY_API_KEY", "");
        String spaceId = System.getenv().getOrDefault("AGENTARTS_MEMORY_SPACE_ID", "");

        // 1. Create Memory client (for state persistence)
        MemoryClient memoryClient = new MemoryClient(region, apiKey);

        // 2. Create agentscope-compatible state store backed by AgentArts Memory
        MemoryAgentStateStore stateStore = new MemoryAgentStateStore(memoryClient, spaceId);

        // 3. Create AgentArts Runtime
        AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();

        // 4. Bridge AgentArts Runtime with agentscope handler
        new AgentscopeRuntimeHost(app, (payload, runtimeCtx) -> {
            String message = (String) payload.getOrDefault("message", "Hello");
            String userId = runtimeCtx.getUserId() != null ? runtimeCtx.getUserId() : "default";
            String sessionId = runtimeCtx.getSessionId() != null ? runtimeCtx.getSessionId() : "default-session";

            // Save conversation state
            ConversationState state = new ConversationState(message, System.currentTimeMillis());
            stateStore.save(userId, sessionId, "last_message", state);

            // Convert AgentArts TextMessage → agentscope TextBlock (demonstration)
            TextMessage agentArtsMsg = new TextMessage("assistant", "Processed: " + message);
            TextBlock textBlock = MessageConverter.toTextBlock(agentArtsMsg);

            return Map.of(
                    "reply", textBlock.getText(),
                    "userId", userId,
                    "sessionId", sessionId
            );
        });

        // 5. Custom ping handler
        app.setPingHandler(() -> PingStatus.HEALTHY);

        // 6. Start the server
        int port = 8080;
        System.out.println("Starting AgentScopeIntegrationExample on port " + port + "...");
        System.out.println("  Health check:  GET  http://localhost:" + port + "/ping");
        System.out.println("  Invocation:    POST http://localhost:" + port + "/invocations");
        app.run(port);
    }

    /**
     * Simple state object implementing agentscope's State marker interface.
     */
    public static class ConversationState implements State {
        public final String message;
        public final long timestamp;

        public ConversationState() {
            this("", 0);
        }

        public ConversationState(String message, long timestamp) {
            this.message = message;
            this.timestamp = timestamp;
        }
    }
}
