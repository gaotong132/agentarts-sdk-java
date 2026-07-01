package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.huaweicloud.agentarts.sdk.core.util.JsonUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool call message for add_messages API.
 *
 * <p>Serializes to OpenAPI "parts" format:
 * {@code {"role":"tool", "parts":[{"type":"tool_call", "tool_call":{id,name,arguments}}]}}</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolCallMessage {

    @JsonProperty("id") private String id = "";
    @JsonProperty("name") private String name = "";
    @JsonProperty("arguments") private String arguments = "";
    @JsonProperty("meta") private String meta;

    public ToolCallMessage() {}
    public ToolCallMessage(String id, String name, Object arguments) {
        this.id = id;
        this.name = name;
        if (arguments instanceof String) {
            this.arguments = (String) arguments;
        } else {
            try {
                this.arguments = JsonUtils.MAPPER.writeValueAsString(arguments);
            } catch (Exception e) {
                this.arguments = String.valueOf(arguments);
            }
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getArguments() { return arguments; }
    public void setArguments(String arguments) { this.arguments = arguments; }
    public String getMeta() { return meta; }
    public void setMeta(String meta) { this.meta = meta; }

    /**
     * Convert to OpenAPI "parts" format:
     * {@code {"role":"tool", "parts":[{"type":"tool_call", "tool_call":{...}}]}}
     */
    public Map<String, Object> toDict() {
        Map<String, Object> toolCall = new HashMap<>();
        toolCall.put("id", id);
        toolCall.put("name", name);
        toolCall.put("arguments", arguments);

        Map<String, Object> part = new HashMap<>();
        part.put("type", "tool_call");
        part.put("tool_call", toolCall);

        Map<String, Object> result = new HashMap<>();
        result.put("role", "tool");
        result.put("parts", List.of(part));
        if (meta != null) result.put("meta", meta);
        return result;
    }
}
