package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Text message for add_messages API.
 *
 * <p>Serializes to OpenAPI "parts" format:
 * {@code {"role":"user", "parts":[{"type":"text", "text":"Hello"}]}}</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TextMessage {
    @JsonProperty("role") private String role = "user";
    @JsonProperty("content") private String content = "";
    @JsonProperty("actor_id") private String actorId;
    @JsonProperty("assistant_id") private String assistantId;
    @JsonProperty("meta") private String meta;

    public TextMessage() {}
    public TextMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public String getAssistantId() { return assistantId; }
    public void setAssistantId(String a) { this.assistantId = a; }
    public String getMeta() { return meta; }
    public void setMeta(String meta) { this.meta = meta; }

    /**
     * Convert to OpenAPI "parts" format:
     * {@code {"role":"user", "parts":[{"type":"text", "text":"Hello"}]}}
     */
    public Map<String, Object> toDict() {
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("Text message content cannot be empty");
        }

        Map<String, Object> part = new HashMap<>();
        part.put("type", "text");
        part.put("text", content);

        Map<String, Object> result = new HashMap<>();
        result.put("role", role);
        result.put("parts", List.of(part));
        if (meta != null) result.put("meta", meta);
        return result;
    }
}
