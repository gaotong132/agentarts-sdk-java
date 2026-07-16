package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Paginated session list response. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionListResponse {
    @JsonProperty("items") private List<SessionInfo> items;
    @JsonProperty("total") private int total;
    @JsonProperty("limit") private int limit = 20;
    @JsonProperty("offset") private int offset;

    public List<SessionInfo> getItems() { return items; }
    public void setItems(List<SessionInfo> items) { this.items = items; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }
}
