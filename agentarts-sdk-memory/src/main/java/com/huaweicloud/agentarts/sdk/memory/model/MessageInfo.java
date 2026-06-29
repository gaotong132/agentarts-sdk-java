package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/** Message information. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageInfo {
    @JsonProperty("id") private String id;
    @JsonProperty("session_id") private String sessionId;
    @JsonProperty("seq") private int seq;
    @JsonProperty("actor_id") private String actorId;
    @JsonProperty("assistant_id") private String assistantId;
    @JsonProperty("role") private String role = "user";
    @JsonProperty("parts") private List<Map<String, Object>> parts;
    @JsonProperty("idempotency_key") private String idempotencyKey;
    @JsonProperty("meta") private Map<String, Object> meta;
    @JsonProperty("message_time") private Long messageTime;
    @JsonProperty("created_at") private String createdAt;

    public String getId() { return id; }
    public String getSessionId() { return sessionId; }
    public int getSeq() { return seq; }
    public String getActorId() { return actorId; }
    public String getAssistantId() { return assistantId; }
    public String getRole() { return role; }
    public List<Map<String, Object>> getParts() { return parts; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Map<String, Object> getMeta() { return meta; }
    public Long getMessageTime() { return messageTime; }
    public String getCreatedAt() { return createdAt; }
}
