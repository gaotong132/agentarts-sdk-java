package com.example;

import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;
import com.huaweicloud.agentarts.sdk.runtime.context.RequestContext;
import com.huaweicloud.agentarts.sdk.core.PingStatus;
import reactor.core.publisher.Flux;
import java.util.Map;

/**
 * AgentScope integration template.
 * Uses ReActAgent with MemoryAgentStateStore and AgentArts tools.
 */
public class {{ name }}Agent {

    /**
     * Factory used by {@code agentarts dev} to load this agent's runtime app.
     * Mirrors the Python {@code create_app} entrypoint contract.
     */
    public static AgentArtsRuntimeApp createApp() {
        AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();

        // TODO: Build ReActAgent with agentscope-java
        // Toolkit toolkit = new Toolkit();
        // toolkit.registerAgentTool(new MCPGatewayTool(gatewayClient));
        // toolkit.registerAgentTool(new CodeInterpreterTool(interpreterClient));
        // ReActAgent agent = ReActAgent.builder()
        //     .name("{{ name }}")
        //     .model("openai:gpt-4o")
        //     .toolkit(toolkit)
        //     .stateStore(new MemoryAgentStateStore(memoryClient))
        //     .build();

        app.setEntrypoint((Map<String, Object> payload, RequestContext ctx) -> {
            String message = (String) payload.getOrDefault("message", "");
            // TODO: Replace with agent.call() or agent.streamEvents()
            return Flux.just(
                Map.of("type", "TEXT_BLOCK_DELTA", "delta", "Thinking..."),
                Map.of("type", "AGENT_RESULT", "result", "Response to: " + message)
            );
        });

        app.setPingHandler(() -> PingStatus.HEALTHY);
        return app;
    }

    public static void main(String[] args) {
        AgentArtsRuntimeApp app = createApp();
        System.out.println("Starting {{ name }} agentscope agent...");
        app.run(8080);
    }
}
