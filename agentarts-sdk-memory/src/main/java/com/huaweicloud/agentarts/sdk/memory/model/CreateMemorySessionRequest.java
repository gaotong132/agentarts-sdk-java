package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/** Request body for creating a memory session. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateMemorySessionRequest {

    @JsonProperty("id") private String id;
    @JsonProperty("actor_id") private String actorId;
    @JsonProperty("assistant_id") private String assistantId;

    public CreateMemorySessionRequest() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public CreateMemorySessionRequest withId(String id) { this.id = id; return this; }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public CreateMemorySessionRequest withActorId(String actorId) { this.actorId = actorId; return this; }

    public String getAssistantId() { return assistantId; }
    public void setAssistantId(String assistantId) { this.assistantId = assistantId; }
    public CreateMemorySessionRequest withAssistantId(String assistantId) { this.assistantId = assistantId; return this; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CreateMemorySessionRequest that)) return false;
        return Objects.equals(id, that.id)
                && Objects.equals(actorId, that.actorId)
                && Objects.equals(assistantId, that.assistantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, actorId, assistantId);
    }

    @Override
    public String toString() {
        return "CreateMemorySessionRequest{"
                + "id='" + id + '\''
                + ", actorId='" + actorId + '\''
                + ", assistantId='" + assistantId + '\''
                + '}';
    }
}
