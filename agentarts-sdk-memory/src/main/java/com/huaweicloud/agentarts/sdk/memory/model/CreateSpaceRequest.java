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
    @JsonProperty("tags") private List<Map<String, String>> tags;
    @JsonProperty("public_access_enable") private Boolean publicAccessEnable;
    @JsonProperty("private_vpc_id") private String privateVpcId;
    @JsonProperty("private_subnet_id") private String privateSubnetId;
    @JsonProperty("network_access") private Map<String, Object> networkAccess;
    @JsonProperty("memory_extract_idle_seconds") private Integer memoryExtractIdleSeconds;
    @JsonProperty("memory_extract_max_tokens") private Integer memoryExtractMaxTokens;
    @JsonProperty("memory_extract_max_messages") private Integer memoryExtractMaxMessages;
    @JsonProperty("memory_strategies_builtin") private List<String> memoryStrategiesBuiltin;
    @JsonProperty("memory_strategies_customized") private List<Map<String, Object>> memoryStrategiesCustomized;
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

    public List<Map<String, String>> getTags() { return tags; }
    public void setTags(List<Map<String, String>> tags) { this.tags = tags; }
    public CreateSpaceRequest withTags(List<Map<String, String>> tags) { this.tags = tags; return this; }

    public Boolean getPublicAccessEnable() { return publicAccessEnable; }
    public void setPublicAccessEnable(Boolean publicAccessEnable) { this.publicAccessEnable = publicAccessEnable; }
    public CreateSpaceRequest withPublicAccessEnable(Boolean publicAccessEnable) { this.publicAccessEnable = publicAccessEnable; return this; }

    public String getPrivateVpcId() { return privateVpcId; }
    public void setPrivateVpcId(String privateVpcId) { this.privateVpcId = privateVpcId; }
    public CreateSpaceRequest withPrivateVpcId(String privateVpcId) { this.privateVpcId = privateVpcId; return this; }

    public String getPrivateSubnetId() { return privateSubnetId; }
    public void setPrivateSubnetId(String privateSubnetId) { this.privateSubnetId = privateSubnetId; }
    public CreateSpaceRequest withPrivateSubnetId(String privateSubnetId) { this.privateSubnetId = privateSubnetId; return this; }

    public Map<String, Object> getNetworkAccess() { return networkAccess; }
    public void setNetworkAccess(Map<String, Object> networkAccess) { this.networkAccess = networkAccess; }
    public CreateSpaceRequest withNetworkAccess(Map<String, Object> networkAccess) { this.networkAccess = networkAccess; return this; }

    public Integer getMemoryExtractIdleSeconds() { return memoryExtractIdleSeconds; }
    public void setMemoryExtractIdleSeconds(Integer memoryExtractIdleSeconds) { this.memoryExtractIdleSeconds = memoryExtractIdleSeconds; }
    public CreateSpaceRequest withMemoryExtractIdleSeconds(Integer memoryExtractIdleSeconds) { this.memoryExtractIdleSeconds = memoryExtractIdleSeconds; return this; }

    public Integer getMemoryExtractMaxTokens() { return memoryExtractMaxTokens; }
    public void setMemoryExtractMaxTokens(Integer memoryExtractMaxTokens) { this.memoryExtractMaxTokens = memoryExtractMaxTokens; }
    public CreateSpaceRequest withMemoryExtractMaxTokens(Integer memoryExtractMaxTokens) { this.memoryExtractMaxTokens = memoryExtractMaxTokens; return this; }

    public Integer getMemoryExtractMaxMessages() { return memoryExtractMaxMessages; }
    public void setMemoryExtractMaxMessages(Integer memoryExtractMaxMessages) { this.memoryExtractMaxMessages = memoryExtractMaxMessages; }
    public CreateSpaceRequest withMemoryExtractMaxMessages(Integer memoryExtractMaxMessages) { this.memoryExtractMaxMessages = memoryExtractMaxMessages; return this; }

    public List<String> getMemoryStrategiesBuiltin() { return memoryStrategiesBuiltin; }
    public void setMemoryStrategiesBuiltin(List<String> memoryStrategiesBuiltin) { this.memoryStrategiesBuiltin = memoryStrategiesBuiltin; }
    public CreateSpaceRequest withMemoryStrategiesBuiltin(List<String> memoryStrategiesBuiltin) { this.memoryStrategiesBuiltin = memoryStrategiesBuiltin; return this; }

    public List<Map<String, Object>> getMemoryStrategiesCustomized() { return memoryStrategiesCustomized; }
    public void setMemoryStrategiesCustomized(List<Map<String, Object>> memoryStrategiesCustomized) { this.memoryStrategiesCustomized = memoryStrategiesCustomized; }
    public CreateSpaceRequest withMemoryStrategiesCustomized(List<Map<String, Object>> memoryStrategiesCustomized) { this.memoryStrategiesCustomized = memoryStrategiesCustomized; return this; }

    public int getMessageTtlHours() { return messageTtlHours; }
    public void setMessageTtlHours(int messageTtlHours) { this.messageTtlHours = messageTtlHours; }
    public CreateSpaceRequest withMessageTtlHours(int messageTtlHours) { this.messageTtlHours = messageTtlHours; return this; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CreateSpaceRequest that)) return false;
        return messageTtlHours == that.messageTtlHours
                && Objects.equals(name, that.name)
                && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, messageTtlHours);
    }

    @Override
    public String toString() {
        return "CreateSpaceRequest{"
                + "name='" + name + '\''
                + ", description='" + description + '\''
                + ", messageTtlHours=" + messageTtlHours
                + '}';
    }
}
