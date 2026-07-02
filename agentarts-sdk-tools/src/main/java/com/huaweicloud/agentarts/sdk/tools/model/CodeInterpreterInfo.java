package com.huaweicloud.agentarts.sdk.tools.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Code Interpreter information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CodeInterpreterInfo {
    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("auth_type") private String authType;
    @JsonProperty("api_key_name") private String apiKeyName;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;
    @JsonProperty("execution_agency_name") private String executionAgencyName;
    @JsonProperty("agent_gateway_id") private String agentGatewayId;
    @JsonProperty("workload_identity") private Map<String, Object> workloadIdentity;
    @JsonProperty("access_endpoint") private String accessEndpoint;
    @JsonProperty("observability") private Map<String, Object> observability;
    @JsonProperty("tags") private List<Map<String, String>> tags;
    @JsonProperty("network_config") private Map<String, Object> networkConfig;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getAuthType() { return authType; }
    public String getApiKeyName() { return apiKeyName; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getExecutionAgencyName() { return executionAgencyName; }
    public String getAgentGatewayId() { return agentGatewayId; }
    public Map<String, Object> getWorkloadIdentity() { return workloadIdentity; }
    public String getAccessEndpoint() { return accessEndpoint; }
    public Map<String, Object> getObservability() { return observability; }
    public List<Map<String, String>> getTags() { return tags; }
    public Map<String, Object> getNetworkConfig() { return networkConfig; }
}
