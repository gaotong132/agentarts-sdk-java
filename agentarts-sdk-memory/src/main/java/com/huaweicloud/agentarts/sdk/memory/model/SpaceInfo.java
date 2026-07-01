package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/** Memory space information. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpaceInfo {
    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("message_ttl_hours") private int messageTtlHours = 168;
    @JsonProperty("status") private String status;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;
    @JsonProperty("memory_extract_enabled") private boolean memoryExtractEnabled;
    @JsonProperty("memory_extract_idle_seconds") private Integer memoryExtractIdleSeconds;
    @JsonProperty("memory_extract_max_tokens") private Integer memoryExtractMaxTokens;
    @JsonProperty("memory_extract_max_messages") private Integer memoryExtractMaxMessages;
    @JsonProperty("memory_strategies_builtin") private List<Object> memoryStrategiesBuiltin;
    @JsonProperty("memory_strategies_customized") private List<Map<String, Object>> memoryStrategiesCustomized;
    @JsonProperty("vpc_id") private String vpcId;
    @JsonProperty("subnet_id") private String subnetId;
    @JsonProperty("public_access") private Map<String, Object> publicAccess;
    @JsonProperty("api_key") private Object apiKey;
    @JsonProperty("api_key_id") private String apiKeyId;
    @JsonProperty("public_domain") private String publicDomain;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public int getMessageTtlHours() { return messageTtlHours; }
    public void setMessageTtlHours(int h) { this.messageTtlHours = h; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public boolean isMemoryExtractEnabled() { return memoryExtractEnabled; }
    public List<Object> getMemoryStrategiesBuiltin() { return memoryStrategiesBuiltin; }
    public List<Map<String, Object>> getMemoryStrategiesCustomized() { return memoryStrategiesCustomized; }
    public String getVpcId() { return vpcId; }
    public String getSubnetId() { return subnetId; }
    public Map<String, Object> getPublicAccess() { return publicAccess; }
    public Object getApiKey() { return apiKey; }
    public void setApiKey(Object apiKey) { this.apiKey = apiKey; }
    public String getApiKeyId() { return apiKeyId; }
    public String getPublicDomain() { return publicDomain; }
}
