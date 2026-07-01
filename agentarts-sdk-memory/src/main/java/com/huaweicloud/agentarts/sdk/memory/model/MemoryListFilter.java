package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

/** Memory list filter for sorting and filtering extracted memories. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MemoryListFilter {
    @JsonProperty("strategy_type") private String strategyType;
    @JsonProperty("strategy_id") private String strategyId;
    @JsonProperty("actor_id") private String actorId;
    @JsonProperty("assistant_id") private String assistantId;
    @JsonProperty("session_id") private String sessionId;
    @JsonProperty("start_time") private Long startTime;
    @JsonProperty("end_time") private Long endTime;
    @JsonProperty("sort_by") private String sortBy;
    @JsonProperty("sort_order") private String sortOrder;

    public String getStrategyType() { return strategyType; }
    public void setStrategyType(String strategyType) { this.strategyType = strategyType; }
    public String getStrategyId() { return strategyId; }
    public void setStrategyId(String strategyId) { this.strategyId = strategyId; }
    public String getActorId() { return actorId; }
    public void setActorId(String a) { this.actorId = a; }
    public String getAssistantId() { return assistantId; }
    public void setAssistantId(String assistantId) { this.assistantId = assistantId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public Long getStartTime() { return startTime; }
    public void setStartTime(Long startTime) { this.startTime = startTime; }
    public Long getEndTime() { return endTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }
    public String getSortBy() { return sortBy; }
    public void setSortBy(String s) { this.sortBy = s; }
    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String s) { this.sortOrder = s; }

    public Map<String, Object> toDict() {
        Map<String, Object> m = new HashMap<>();
        if (strategyType != null) m.put("strategy_type", strategyType);
        if (strategyId != null) m.put("strategy_id", strategyId);
        if (actorId != null) m.put("actor_id", actorId);
        if (assistantId != null) m.put("assistant_id", assistantId);
        if (sessionId != null) m.put("session_id", sessionId);
        if (startTime != null) m.put("start_time", startTime);
        if (endTime != null) m.put("end_time", endTime);
        if (sortBy != null) m.put("sort_by", sortBy);
        if (sortOrder != null) m.put("sort_order", sortOrder);
        return m;
    }
}
