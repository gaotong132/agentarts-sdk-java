package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool result message for add_messages API.
 *
 * <p>Serializes to OpenAPI "parts" format:
 * {@code {"role":"tool", "parts":[{"type":"tool_result", "tool_result":{tool_call_id,content,asset_ref}}]}}</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolResultMessage {
    @JsonProperty("tool_call_id") private String toolCallId = "";
    @JsonProperty("content") private String content = "";
    @JsonProperty("asset_ref") private Object assetRef;
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
    public Object getAssetRef() { return assetRef; }
    public void setAssetRef(Object assetRef) { this.assetRef = assetRef; }
    public String getMeta() { return meta; }
    public void setMeta(String meta) { this.meta = meta; }

    /**
     * Convert to OpenAPI "parts" format:
     * {@code {"role":"tool", "parts":[{"type":"tool_result", "tool_result":{...}}]}}
     */
    public Map<String, Object> toDict() {
        Map<String, Object> toolResult = new HashMap<>();
        toolResult.put("tool_call_id", toolCallId);
        toolResult.put("content", content);
        toolResult.put("asset_ref", assetRef);

        Map<String, Object> part = new HashMap<>();
        part.put("type", "tool_result");
        part.put("tool_result", toolResult);

        Map<String, Object> result = new HashMap<>();
        result.put("role", "tool");
        result.put("parts", List.of(part));
        if (meta != null) result.put("meta", meta);
        return result;
    }
}
