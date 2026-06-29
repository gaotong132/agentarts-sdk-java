package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

/** Memory search filter. Mirrors Python MemorySearchFilter. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MemorySearchFilter {
    @JsonProperty("query") private String query;
    @JsonProperty("strategy_type") private String strategyType;
    @JsonProperty("strategy_id") private String strategyId;
    @JsonProperty("actor_id") private String actorId;
    @JsonProperty("assistant_id") private String assistantId;
    @JsonProperty("session_id") private String sessionId;
    @JsonProperty("memory_type") private String memoryType;
    @JsonProperty("start_time") private Long startTime;
    @JsonProperty("end_time") private Long endTime;
    @JsonProperty("top_k") private Integer topK;
    @JsonProperty("min_score") private Double minScore;

    public String getQuery() { return query; }
    public void setQuery(String q) { this.query = q; }
    public String getActorId() { return actorId; }
    public void setActorId(String a) { this.actorId = a; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer k) { this.topK = k; }
    public Double getMinScore() { return minScore; }
    public void setMinScore(Double s) { this.minScore = s; }

    public Map<String, Object> toDict() {
        Map<String, Object> m = new HashMap<>();
        if (query != null) m.put("query", query);
        if (strategyType != null) m.put("strategy_type", strategyType);
        if (strategyId != null) m.put("strategy_id", strategyId);
        if (actorId != null) m.put("actor_id", actorId);
        if (assistantId != null) m.put("assistant_id", assistantId);
        if (sessionId != null) m.put("session_id", sessionId);
        if (memoryType != null) m.put("memory_type", memoryType);
        if (startTime != null) m.put("start_time", startTime);
        if (endTime != null) m.put("end_time", endTime);
        if (topK != null) m.put("top_k", topK);
        if (minScore != null) m.put("min_score", minScore);
        return m;
    }
}
