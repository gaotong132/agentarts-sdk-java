package com.huaweicloud.agentarts.sdk.service.runtime.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response for listing runtime agents.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentListResponse {
    @JsonProperty("items") private List<AgentInfo> items;
    @JsonProperty("total") private int total;
    @JsonProperty("limit") private int limit;
    @JsonProperty("offset") private int offset;

    public List<AgentInfo> getItems() { return items; }
    public int getTotal() { return total; }
    public int getLimit() { return limit; }
    public int getOffset() { return offset; }
}
