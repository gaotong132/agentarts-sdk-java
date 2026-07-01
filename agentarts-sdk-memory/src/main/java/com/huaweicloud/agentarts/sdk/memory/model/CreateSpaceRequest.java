package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Request body for creating a memory space. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateSpaceRequest {

    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("api_key_id") private String apiKeyId;
    @JsonProperty("network_access") private Map<String, Object> networkAccess;
    @JsonProperty("memory_strategies_builtin") private List<String> memoryStrategiesBuiltin;
    @JsonProperty("message_ttl_hours") private int messageTtlHours;

    public CreateSpaceRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public CreateSpaceRequest withName(String name) { this.name = name; return this; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public CreateSpaceRequest withDescription(String description) { this.description = description; return this; }

    public String getApiKeyId() { return apiKeyId; }
    public void setApiKeyId(String apiKeyId) { this.apiKeyId = apiKeyId; }
    public CreateSpaceRequest withApiKeyId(String apiKeyId) { this.apiKeyId = apiKeyId; return this; }

    public Map<String, Object> getNetworkAccess() { return networkAccess; }
    public void setNetworkAccess(Map<String, Object> networkAccess) { this.networkAccess = networkAccess; }
    public CreateSpaceRequest withNetworkAccess(Map<String, Object> networkAccess) { this.networkAccess = networkAccess; return this; }

    public List<String> getMemoryStrategiesBuiltin() { return memoryStrategiesBuiltin; }
    public void setMemoryStrategiesBuiltin(List<String> memoryStrategiesBuiltin) { this.memoryStrategiesBuiltin = memoryStrategiesBuiltin; }
    public CreateSpaceRequest withMemoryStrategiesBuiltin(List<String> memoryStrategiesBuiltin) { this.memoryStrategiesBuiltin = memoryStrategiesBuiltin; return this; }

    public int getMessageTtlHours() { return messageTtlHours; }
    public void setMessageTtlHours(int messageTtlHours) { this.messageTtlHours = messageTtlHours; }
    public CreateSpaceRequest withMessageTtlHours(int messageTtlHours) { this.messageTtlHours = messageTtlHours; return this; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CreateSpaceRequest that)) return false;
        return messageTtlHours == that.messageTtlHours
                && Objects.equals(name, that.name)
                && Objects.equals(description, that.description)
                && Objects.equals(apiKeyId, that.apiKeyId)
                && Objects.equals(networkAccess, that.networkAccess)
                && Objects.equals(memoryStrategiesBuiltin, that.memoryStrategiesBuiltin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, apiKeyId, networkAccess, memoryStrategiesBuiltin, messageTtlHours);
    }

    @Override
    public String toString() {
        return "CreateSpaceRequest{"
                + "name='" + name + '\''
                + ", description='" + description + '\''
                + ", apiKeyId='" + apiKeyId + '\''
                + ", networkAccess=" + networkAccess
                + ", memoryStrategiesBuiltin=" + memoryStrategiesBuiltin
                + ", messageTtlHours=" + messageTtlHours
                + '}';
    }
}
