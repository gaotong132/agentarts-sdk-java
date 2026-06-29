package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Memory information. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MemoryInfo {
    @JsonProperty("id") private String id;
    @JsonProperty("space_id") private String spaceId;
    @JsonProperty("strategy_id") private String strategyId;
    @JsonProperty("strategy_type") private String strategyType;
    @JsonProperty("actor_id") private String actorId;
    @JsonProperty("assistant_id") private String assistantId;
    @JsonProperty("session_id") private String sessionId;
    @JsonProperty("content") private String content = "";
    @JsonProperty("memory_type") private String memoryType = "memory";
    @JsonProperty("isolation_level") private String isolationLevel = "actor";
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    public String getId() { return id; }
    public String getSpaceId() { return spaceId; }
    public String getStrategyId() { return strategyId; }
    public String getStrategyType() { return strategyType; }
    public String getActorId() { return actorId; }
    public String getAssistantId() { return assistantId; }
    public String getSessionId() { return sessionId; }
    public String getContent() { return content; }
    public String getMemoryType() { return memoryType; }
    public String getIsolationLevel() { return isolationLevel; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
