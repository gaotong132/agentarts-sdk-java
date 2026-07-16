package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Request body for updating a memory space. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateSpaceRequest {

    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("message_ttl_hours") private Integer messageTtlHours;
    @JsonProperty("memory_extract_enabled") private Boolean memoryExtractEnabled;
    @JsonProperty("memory_extract_idle_seconds") private Integer memoryExtractIdleSeconds;
    @JsonProperty("memory_extract_max_tokens") private Integer memoryExtractMaxTokens;
    @JsonProperty("memory_extract_max_messages") private Integer memoryExtractMaxMessages;
    @JsonProperty("tags") private List<Map<String, String>> tags;
    @JsonProperty("memory_strategies_builtin") private List<String> memoryStrategiesBuiltin;

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

    public Boolean getMemoryExtractEnabled() { return memoryExtractEnabled; }
    public void setMemoryExtractEnabled(Boolean memoryExtractEnabled) { this.memoryExtractEnabled = memoryExtractEnabled; }
    public UpdateSpaceRequest withMemoryExtractEnabled(Boolean memoryExtractEnabled) { this.memoryExtractEnabled = memoryExtractEnabled; return this; }

    public Integer getMemoryExtractIdleSeconds() { return memoryExtractIdleSeconds; }
    public void setMemoryExtractIdleSeconds(Integer memoryExtractIdleSeconds) { this.memoryExtractIdleSeconds = memoryExtractIdleSeconds; }
    public UpdateSpaceRequest withMemoryExtractIdleSeconds(Integer memoryExtractIdleSeconds) { this.memoryExtractIdleSeconds = memoryExtractIdleSeconds; return this; }

    public Integer getMemoryExtractMaxTokens() { return memoryExtractMaxTokens; }
    public void setMemoryExtractMaxTokens(Integer memoryExtractMaxTokens) { this.memoryExtractMaxTokens = memoryExtractMaxTokens; }
    public UpdateSpaceRequest withMemoryExtractMaxTokens(Integer memoryExtractMaxTokens) { this.memoryExtractMaxTokens = memoryExtractMaxTokens; return this; }

    public Integer getMemoryExtractMaxMessages() { return memoryExtractMaxMessages; }
    public void setMemoryExtractMaxMessages(Integer memoryExtractMaxMessages) { this.memoryExtractMaxMessages = memoryExtractMaxMessages; }
    public UpdateSpaceRequest withMemoryExtractMaxMessages(Integer memoryExtractMaxMessages) { this.memoryExtractMaxMessages = memoryExtractMaxMessages; return this; }

    public List<Map<String, String>> getTags() { return tags; }
    public void setTags(List<Map<String, String>> tags) { this.tags = tags; }
    public UpdateSpaceRequest withTags(List<Map<String, String>> tags) { this.tags = tags; return this; }

    public List<String> getMemoryStrategiesBuiltin() { return memoryStrategiesBuiltin; }
    public void setMemoryStrategiesBuiltin(List<String> memoryStrategiesBuiltin) { this.memoryStrategiesBuiltin = memoryStrategiesBuiltin; }
    public UpdateSpaceRequest withMemoryStrategiesBuiltin(List<String> memoryStrategiesBuiltin) { this.memoryStrategiesBuiltin = memoryStrategiesBuiltin; return this; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UpdateSpaceRequest that)) return false;
        return Objects.equals(name, that.name)
                && Objects.equals(description, that.description)
                && Objects.equals(messageTtlHours, that.messageTtlHours)
                && Objects.equals(memoryExtractEnabled, that.memoryExtractEnabled)
                && Objects.equals(memoryExtractIdleSeconds, that.memoryExtractIdleSeconds)
                && Objects.equals(memoryExtractMaxTokens, that.memoryExtractMaxTokens)
                && Objects.equals(memoryExtractMaxMessages, that.memoryExtractMaxMessages)
                && Objects.equals(tags, that.tags)
                && Objects.equals(memoryStrategiesBuiltin, that.memoryStrategiesBuiltin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                name,
                description,
                messageTtlHours,
                memoryExtractEnabled,
                memoryExtractIdleSeconds,
                memoryExtractMaxTokens,
                memoryExtractMaxMessages,
                tags,
                memoryStrategiesBuiltin);
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
