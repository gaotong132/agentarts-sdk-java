package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Request body for adding messages to a session. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddMessagesRequest {

    @JsonProperty("messages") private List<Map<String, Object>> messages;
    @JsonProperty("timestamp") private Long timestamp;
    @JsonProperty("idempotency_key") private String idempotencyKey;
    @JsonProperty("is_force_extract") private boolean isForceExtract;

    public AddMessagesRequest() {}

    public List<Map<String, Object>> getMessages() { return messages; }
    public void setMessages(List<Map<String, Object>> messages) { this.messages = messages; }
    public AddMessagesRequest withMessages(List<Map<String, Object>> messages) { this.messages = messages; return this; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    public AddMessagesRequest withTimestamp(Long timestamp) { this.timestamp = timestamp; return this; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public AddMessagesRequest withIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }

    public boolean isIsForceExtract() { return isForceExtract; }
    public void setIsForceExtract(boolean isForceExtract) { this.isForceExtract = isForceExtract; }
    public AddMessagesRequest withIsForceExtract(boolean isForceExtract) { this.isForceExtract = isForceExtract; return this; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AddMessagesRequest that)) return false;
        return isForceExtract == that.isForceExtract
                && Objects.equals(messages, that.messages)
                && Objects.equals(timestamp, that.timestamp)
                && Objects.equals(idempotencyKey, that.idempotencyKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messages, timestamp, idempotencyKey, isForceExtract);
    }

    @Override
    public String toString() {
        return "AddMessagesRequest{"
                + "messages=" + (messages == null
                ? "null" : "[REDACTED, count=" + messages.size() + "]")
                + ", timestamp=" + timestamp
                + ", idempotencyKey=" + (idempotencyKey == null ? "null" : "[REDACTED]")
                + ", isForceExtract=" + isForceExtract
                + '}';
    }
}
