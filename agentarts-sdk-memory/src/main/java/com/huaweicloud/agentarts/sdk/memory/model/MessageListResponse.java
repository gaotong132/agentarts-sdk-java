package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Response for listing messages. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageListResponse {
    @JsonProperty("items") private List<MessageInfo> items;
    @JsonProperty("total") private int total;
    @JsonProperty("limit") private int limit;
    @JsonProperty("offset") private int offset;

    public List<MessageInfo> getItems() { return items; }
    public int getTotal() { return total; }
    public int getLimit() { return limit; }
    public int getOffset() { return offset; }
}
