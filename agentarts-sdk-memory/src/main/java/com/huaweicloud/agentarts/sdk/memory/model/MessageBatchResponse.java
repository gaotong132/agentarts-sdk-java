package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Response for batch adding messages. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageBatchResponse {
    @JsonProperty("items") private List<MessageInfo> items;

    public List<MessageInfo> getItems() { return items; }
}
