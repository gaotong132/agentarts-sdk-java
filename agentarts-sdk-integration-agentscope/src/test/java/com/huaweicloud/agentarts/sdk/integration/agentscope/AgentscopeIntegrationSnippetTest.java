package com.huaweicloud.agentarts.sdk.integration.agentscope;

import com.huaweicloud.agentarts.sdk.integration.agentscope.runtime.AgentscopeRuntimeHost;
import com.huaweicloud.agentarts.sdk.integration.agentscope.state.MemoryAgentStateStore;
import com.huaweicloud.agentarts.sdk.integration.agentscope.tool.CodeInterpreterTool;
import com.huaweicloud.agentarts.sdk.integration.agentscope.tool.MCPGatewayTool;
import com.huaweicloud.agentarts.sdk.memory.MemoryClient;
import com.huaweicloud.agentarts.sdk.mcpgateway.MCPGatewayClient;
import com.huaweicloud.agentarts.sdk.runtime.AgentArtsRuntimeApp;
import com.huaweicloud.agentarts.sdk.tools.CodeInterpreterClient;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.tool.Toolkit;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Compiles + constructs the README "agentscope-java Integration" snippet
 * end-to-end (without starting the HTTP server or making cloud/model calls),
 * so the documented wiring stays verifiably buildable.
 */
class AgentscopeIntegrationSnippetTest {

    @Test
    void snippetConstructsSuccessfully() {
        // Clients — construction is cloud-free; AK/SK are read lazily at sign time.
        MCPGatewayClient gatewayClient = new MCPGatewayClient();
        CodeInterpreterClient interpreterClient = new CodeInterpreterClient("cn-southwest-2");
        MemoryClient memoryClient = new MemoryClient("cn-southwest-2", "dummy-api-key");
        String spaceId = "dummy-space";

        // Register AgentArts tools (MCP Gateway, Code Interpreter) as agentscope AgentTools.
        Toolkit toolkit = new Toolkit();
        toolkit.registerAgentTool(new MCPGatewayTool(gatewayClient));
        toolkit.registerAgentTool(new CodeInterpreterTool(interpreterClient));

        // Use AgentArts Memory as the agent's state store (client + spaceId).
        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey("dummy-key").modelName("gpt-4o").stream(true).build();

        ReActAgent agent = ReActAgent.builder()
                .name("my-agent").model(model).toolkit(toolkit)
                .stateStore(new MemoryAgentStateStore(memoryClient, spaceId))
                .build();
        // build() success is the smoke test — no filler assertNotNull needed.

        // Host the agent behind AgentArts Runtime — POST /invocations dispatches to it.
        // (app.run(port) omitted — it would block on the HTTP server.)
        AgentArtsRuntimeApp app = new AgentArtsRuntimeApp();
        new AgentscopeRuntimeHost(app, (payload, ctx) -> {
            String message = (String) payload.getOrDefault("message", "");
            var reply = agent.call(message, ctx).block();
            return Map.of("reply", reply != null ? reply.getTextContent() : "");
        });
        // Construction of AgentscopeRuntimeHost is the smoke test — no filler assertNotNull.

        agent.close();
        gatewayClient.close();
        interpreterClient.close();
        memoryClient.close();
    }
}
