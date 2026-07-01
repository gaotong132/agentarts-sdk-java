package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Response for listing spaces. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpaceListResponse {
    @JsonProperty("spaces") private List<SpaceInfo> items;
    @JsonProperty("total") private int total;
    @JsonProperty("size") private int limit;
    @JsonProperty("offset") private int offset;

    public List<SpaceInfo> getItems() { return items; }
    public int getTotal() { return total; }
    public int getLimit() { return limit; }
    public int getOffset() { return offset; }
}
