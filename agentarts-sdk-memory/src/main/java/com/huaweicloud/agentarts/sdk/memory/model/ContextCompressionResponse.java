package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Result of a context compression request. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContextCompressionResponse {
    @JsonProperty("compression_id") private String compressionId;
    @JsonProperty("status") private String status;
    @JsonProperty("compressed_messages") private List<MessageInfo> compressedMessages;
    @JsonProperty("compression_ratio") private Double compressionRatio;
    @JsonProperty("original_token_count") private Integer originalTokenCount;
    @JsonProperty("compressed_token_count") private Integer compressedTokenCount;

    public String getCompressionId() { return compressionId; }
    public void setCompressionId(String compressionId) { this.compressionId = compressionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<MessageInfo> getCompressedMessages() { return compressedMessages; }
    public void setCompressedMessages(List<MessageInfo> value) { this.compressedMessages = value; }
    public Double getCompressionRatio() { return compressionRatio; }
    public void setCompressionRatio(Double compressionRatio) { this.compressionRatio = compressionRatio; }
    public Integer getOriginalTokenCount() { return originalTokenCount; }
    public void setOriginalTokenCount(Integer originalTokenCount) { this.originalTokenCount = originalTokenCount; }
    public Integer getCompressedTokenCount() { return compressedTokenCount; }
    public void setCompressedTokenCount(Integer value) { this.compressedTokenCount = value; }
}
