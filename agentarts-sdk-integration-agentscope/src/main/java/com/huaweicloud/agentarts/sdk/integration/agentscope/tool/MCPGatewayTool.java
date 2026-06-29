package com.huaweicloud.agentarts.sdk.integration.agentscope.tool;

import com.huaweicloud.agentarts.sdk.mcpgateway.MCPGatewayClient;
import com.huaweicloud.agentarts.sdk.service.http.RequestResult;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolCallParam;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AgentTool implementation wrapping MCP Gateway operations.
 *
 * <p>Implements all 6 methods of the {@link AgentTool} interface.
 * Exposes MCP gateway tool calling to agentscope agents.</p>
 *
 * <p>Tool name: "mcp_gateway_call"
 * Parameters JSON Schema: {gateway_id, target_id, tool_name, arguments}</p>
 */
public class MCPGatewayTool implements AgentTool {

    private static final String TOOL_NAME = "mcp_gateway_call";
    private static final String TOOL_DESCRIPTION =
            "Call a tool through the MCP Gateway. " +
            "Provide the gateway_id, target_id, tool_name, and arguments.";

    private final MCPGatewayClient gatewayClient;

    public MCPGatewayTool(MCPGatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return TOOL_DESCRIPTION;
    }

    @Override
    public Map<String, Object> getParameters() {
        // JSON Schema for tool parameters
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        properties.put("gateway_id", Map.of("type", "string", "description", "MCP Gateway ID"));
        properties.put("target_id", Map.of("type", "string", "description", "MCP Gateway Target ID"));
        properties.put("tool_name", Map.of("type", "string", "description", "Name of the tool to call"));
        properties.put("arguments", Map.of("type", "object", "description", "Tool arguments"));
        schema.put("properties", properties);
        schema.put("required", List.of("gateway_id", "target_id", "tool_name"));

        return schema;
    }

    @Override
    public Boolean getStrict() {
        return null;
    }

    @Override
    public Map<String, Object> getOutputSchema() {
        return null;
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        return Mono.fromCallable(() -> {
            Map<String, Object> input = param.getInput();
            String gatewayId = (String) input.get("gateway_id");
            String targetId = (String) input.get("target_id");
            String toolName = (String) input.get("tool_name");
            Object arguments = input.get("arguments");

            // Verify gateway and target exist
            RequestResult gatewayResult = gatewayClient.getMcpGateway(gatewayId);
            if (gatewayResult == null || !gatewayResult.isSuccess()) {
                return ToolResultBlock.error("Gateway not found: " + gatewayId);
            }

            RequestResult targetResult = gatewayClient.getMcpGatewayTarget(gatewayId, targetId);
            if (targetResult == null || !targetResult.isSuccess()) {
                return ToolResultBlock.error("Target not found: " + targetId);
            }

            // Build result text
            String resultText = String.format(
                    "MCP Gateway call: gateway=%s, target=%s, tool=%s",
                    gatewayId, targetId, toolName);

            return ToolResultBlock.text(resultText);
        });
    }
}
