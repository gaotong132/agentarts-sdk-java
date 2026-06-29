package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

/** Tool call message for add_messages. Mirrors Python ToolCallMessage. */
public class ToolCallMessage {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @JsonProperty("type") private String type = "tool_call";
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
                this.arguments = MAPPER.writeValueAsString(arguments);
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

    public Map<String, Object> toDict() {
        Map<String, Object> m = new HashMap<>();
        m.put("type", type);
        m.put("id", id);
        m.put("name", name);
        m.put("arguments", arguments);
        if (meta != null) m.put("meta", meta);
        return m;
    }
}
