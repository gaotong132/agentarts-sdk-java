package com.huaweicloud.agentarts.sdk.integration.agentscope.tool;

import com.huaweicloud.agentarts.sdk.core.util.JsonUtils;
import com.huaweicloud.agentarts.sdk.tools.CodeInterpreterClient;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolCallParam;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        this.interpreterClient = Objects.requireNonNull(interpreterClient, "interpreterClient");
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
    public Map<String, Object> getOutputSchema() {
        return null;
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        return Mono.fromCallable(() -> {
            if (param == null || param.getInput() == null) {
                return error("Tool input is required");
            }
            Map<String, Object> input = param.getInput();
            Object codeValue = input.get("code");
            Object languageValue = input.getOrDefault("language", "python");

            if (!(codeValue instanceof String) || ((String) codeValue).isBlank()) {
                return error("Code is required");
            }
            if (!(languageValue instanceof String) || ((String) languageValue).isBlank()) {
                return error("Language must be a non-blank string");
            }

            try {
                Map<String, Object> result = interpreterClient.executeCode(
                        (String) codeValue, (String) languageValue, false);
                String output = result != null ? JsonUtils.toJson(result) : "No output";
                return ToolResultBlock.text(output);
            } catch (Exception e) {
                return error("Code execution failed");
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private static ToolResultBlock error(String message) {
        return ToolResultBlock.error(message);
    }
}
