package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/** Response for searching memories. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MemorySearchResponse {
    @JsonProperty("results") private List<Map<String, Object>> results;
    @JsonProperty("total") private int total = 0;
    @JsonProperty("query") private String query;

    public List<Map<String, Object>> getResults() { return results; }
    public int getTotal() { return total; }
    public String getQuery() { return query; }
}
