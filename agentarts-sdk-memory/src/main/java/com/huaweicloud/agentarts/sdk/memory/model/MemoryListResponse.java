package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Response for listing memories. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MemoryListResponse {
    @JsonProperty("items") private List<MemoryInfo> items;
    @JsonProperty("total") private int total;
    @JsonProperty("limit") private int limit;
    @JsonProperty("offset") private int offset;

    public List<MemoryInfo> getItems() { return items; }
    public int getTotal() { return total; }
    public int getLimit() { return limit; }
    public int getOffset() { return offset; }
}
