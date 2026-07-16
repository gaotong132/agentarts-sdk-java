package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Advanced message request composed of one to five pre-serialized message parts. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageRequest {
    @JsonProperty("role") private String role;
    @JsonProperty("parts") private List<Map<String, Object>> parts;
    @JsonProperty("actor_id") private String actorId;
    @JsonProperty("assistant_id") private String assistantId;
    @JsonProperty("meta") private Map<String, Object> meta;

    public MessageRequest() {}

    public MessageRequest(String role, List<Map<String, Object>> parts) {
        this.role = role;
        setParts(parts);
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public MessageRequest withRole(String role) { this.role = role; return this; }
    public List<Map<String, Object>> getParts() { return parts; }
    public void setParts(List<Map<String, Object>> parts) {
        validateParts(parts);
        this.parts = List.copyOf(parts);
    }
    public MessageRequest withParts(List<Map<String, Object>> parts) {
        setParts(parts);
        return this;
    }
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public MessageRequest withActorId(String actorId) { this.actorId = actorId; return this; }
    public String getAssistantId() { return assistantId; }
    public void setAssistantId(String assistantId) { this.assistantId = assistantId; }
    public MessageRequest withAssistantId(String assistantId) { this.assistantId = assistantId; return this; }
    public Map<String, Object> getMeta() { return meta; }
    public void setMeta(Map<String, Object> meta) { this.meta = meta; }
    public MessageRequest withMeta(Map<String, Object> meta) { this.meta = meta; return this; }

    /** Convert to the Memory service wire representation. */
    public Map<String, Object> toDict() {
        validateParts(parts);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("role", role);
        result.put("parts", parts);
        if (actorId != null) result.put("actor_id", actorId);
        if (assistantId != null) result.put("assistant_id", assistantId);
        if (meta != null) result.put("meta", meta);
        return result;
    }

    private static void validateParts(List<Map<String, Object>> parts) {
        if (parts == null || parts.isEmpty()) {
            throw new IllegalArgumentException("Message must contain at least one part");
        }
        if (parts.size() > 5) {
            throw new IllegalArgumentException("Message can contain at most 5 parts");
        }
        if (parts.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Message parts must not contain null");
        }
    }
}
