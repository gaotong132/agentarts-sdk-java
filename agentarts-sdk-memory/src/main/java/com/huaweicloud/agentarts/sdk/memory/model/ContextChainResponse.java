package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Context chain returned by the Memory service. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContextChainResponse {
    @JsonProperty("messages") private List<MessageInfo> messages;
    @JsonProperty("total_token_count") private int totalTokenCount;
    @JsonProperty("compressed") private boolean compressed;

    public List<MessageInfo> getMessages() { return messages; }
    public void setMessages(List<MessageInfo> messages) { this.messages = messages; }
    public int getTotalTokenCount() { return totalTokenCount; }
    public void setTotalTokenCount(int totalTokenCount) { this.totalTokenCount = totalTokenCount; }
    public boolean isCompressed() { return compressed; }
    public void setCompressed(boolean compressed) { this.compressed = compressed; }
}
