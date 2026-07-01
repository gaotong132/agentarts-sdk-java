package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Response for batch adding messages. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageBatchResponse {
    @JsonProperty("messages") private List<MessageInfo> items;
    @JsonProperty("count") private int count;

    public List<MessageInfo> getItems() { return items; }
    public int getCount() { return count; }
}
