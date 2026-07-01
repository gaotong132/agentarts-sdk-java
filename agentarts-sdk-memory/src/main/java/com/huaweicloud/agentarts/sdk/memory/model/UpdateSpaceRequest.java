package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/** Request body for updating a memory space. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateSpaceRequest {

    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("message_ttl_hours") private Integer messageTtlHours;

    public UpdateSpaceRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UpdateSpaceRequest withName(String name) { this.name = name; return this; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public UpdateSpaceRequest withDescription(String description) { this.description = description; return this; }

    public Integer getMessageTtlHours() { return messageTtlHours; }
    public void setMessageTtlHours(Integer messageTtlHours) { this.messageTtlHours = messageTtlHours; }
    public UpdateSpaceRequest withMessageTtlHours(Integer messageTtlHours) { this.messageTtlHours = messageTtlHours; return this; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UpdateSpaceRequest that)) return false;
        return Objects.equals(name, that.name)
                && Objects.equals(description, that.description)
                && Objects.equals(messageTtlHours, that.messageTtlHours);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, messageTtlHours);
    }

    @Override
    public String toString() {
        return "UpdateSpaceRequest{"
                + "name='" + name + '\''
                + ", description='" + description + '\''
                + ", messageTtlHours=" + messageTtlHours
                + '}';
    }
}
