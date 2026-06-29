package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/** Session information. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionInfo {
    @JsonProperty("id") private String id;
    @JsonProperty("space_id") private String spaceId;
    @JsonProperty("actor_id") private String actorId;
    @JsonProperty("assistant_id") private String assistantId;
    @JsonProperty("meta") private Map<String, Object> meta;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSpaceId() { return spaceId; }
    public String getActorId() { return actorId; }
    public String getAssistantId() { return assistantId; }
    public Map<String, Object> getMeta() { return meta; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
