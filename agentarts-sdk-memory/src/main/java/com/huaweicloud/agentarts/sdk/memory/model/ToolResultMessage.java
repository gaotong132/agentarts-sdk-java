package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

/** Tool result message for add_messages. Mirrors Python ToolResultMessage. */
public class ToolResultMessage {
    @JsonProperty("type") private String type = "tool_result";
    @JsonProperty("tool_call_id") private String toolCallId = "";
    @JsonProperty("content") private String content = "";
    @JsonProperty("meta") private String meta;

    public ToolResultMessage() {}
    public ToolResultMessage(String toolCallId, String content) {
        this.toolCallId = toolCallId;
        this.content = content;
    }

    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String id) { this.toolCallId = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Map<String, Object> toDict() {
        Map<String, Object> m = new HashMap<>();
        m.put("type", type);
        m.put("tool_call_id", toolCallId);
        m.put("content", content);
        if (meta != null) m.put("meta", meta);
        return m;
    }
}
