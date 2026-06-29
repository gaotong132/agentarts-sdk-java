package com.huaweicloud.agentarts.sdk.integration.agentscope.tool;

import com.huaweicloud.agentarts.sdk.tools.CodeInterpreterClient;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolCallParam;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AgentTool implementation wrapping Code Interpreter operations.
 *
 * <p>Implements all 6 methods of the {@link AgentTool} interface.
 * Exposes code execution capabilities to agentscope agents.</p>
 *
 * <p>Tool name: "code_interpreter"
 * Parameters JSON Schema: {code, language}</p>
 */
public class CodeInterpreterTool implements AgentTool {

    private static final String TOOL_NAME = "code_interpreter";
    private static final String TOOL_DESCRIPTION =
            "Execute code in a sandboxed code interpreter. " +
            "Provide the code and optionally the language (default: python).";

    private final CodeInterpreterClient interpreterClient;

    public CodeInterpreterTool(CodeInterpreterClient interpreterClient) {
        this.interpreterClient = interpreterClient;
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
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        properties.put("code", Map.of("type", "string", "description", "Code to execute"));
        properties.put("language", Map.of("type", "string", "description", "Programming language (default: python)"));
        schema.put("properties", properties);
        schema.put("required", List.of("code"));

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
            String code = (String) input.get("code");
            String language = (String) input.getOrDefault("language", "python");

            if (code == null || code.isEmpty()) {
                return ToolResultBlock.error("Code is required");
            }

            try {
                Map<String, Object> result = interpreterClient.executeCode(code, language, false);
                String output = result != null ? result.toString() : "No output";
                return ToolResultBlock.text(output);
            } catch (Exception e) {
                return ToolResultBlock.error("Code execution failed: " + e.getMessage());
            }
        });
    }
}
