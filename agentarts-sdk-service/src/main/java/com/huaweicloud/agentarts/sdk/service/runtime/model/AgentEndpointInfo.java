package com.huaweicloud.agentarts.sdk.service.runtime.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Runtime agent endpoint information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentEndpointInfo {
    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("agent_id") private String agentId;
    @JsonProperty("endpoint_type") private String endpointType;
    @JsonProperty("target_version_name") private String targetVersionName;
    @JsonProperty("config") private Map<String, Object> config;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getAgentId() { return agentId; }
    public String getEndpointType() { return endpointType; }
    public String getTargetVersionName() { return targetVersionName; }
    public Map<String, Object> getConfig() { return config; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
