package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

/** Text message for add_messages. Mirrors Python TextMessage. */
public class TextMessage {
    @JsonProperty("type") private String type = "text";
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

    public Map<String, Object> toDict() {
        Map<String, Object> m = new HashMap<>();
        m.put("type", type);
        m.put("role", role);
        m.put("content", content);
        if (actorId != null) m.put("actor_id", actorId);
        if (assistantId != null) m.put("assistant_id", assistantId);
        if (meta != null) m.put("meta", meta);
        return m;
    }
}
