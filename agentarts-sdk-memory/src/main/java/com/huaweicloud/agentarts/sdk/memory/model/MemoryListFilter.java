package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

/** Memory list filter. Mirrors Python MemoryListFilter. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MemoryListFilter {
    @JsonProperty("strategy_type") private String strategyType;
    @JsonProperty("actor_id") private String actorId;
    @JsonProperty("session_id") private String sessionId;
    @JsonProperty("sort_by") private String sortBy;
    @JsonProperty("sort_order") private String sortOrder;

    public String getActorId() { return actorId; }
    public void setActorId(String a) { this.actorId = a; }
    public String getSortBy() { return sortBy; }
    public void setSortBy(String s) { this.sortBy = s; }
    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String s) { this.sortOrder = s; }

    public Map<String, Object> toDict() {
        Map<String, Object> m = new HashMap<>();
        if (strategyType != null) m.put("strategy_type", strategyType);
        if (actorId != null) m.put("actor_id", actorId);
        if (sessionId != null) m.put("session_id", sessionId);
        if (sortBy != null) m.put("sort_by", sortBy);
        if (sortOrder != null) m.put("sort_order", sortOrder);
        return m;
    }
}
