package com.huaweicloud.agentarts.sdk.service.runtime.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Runtime agent information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentInfo {
    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("status") private String status;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;
    @JsonProperty("artifact_source") private Map<String, Object> artifactSource;
    @JsonProperty("identity_configuration") private Map<String, Object> identityConfiguration;
    @JsonProperty("invoke_config") private Map<String, Object> invokeConfig;
    @JsonProperty("network_config") private Map<String, Object> networkConfig;
    @JsonProperty("observability") private Map<String, Object> observability;
    @JsonProperty("execution_agency_name") private String executionAgencyName;
    @JsonProperty("agent_gateway_id") private String agentGatewayId;
    @JsonProperty("workload_identity") private Map<String, Object> workloadIdentity;
    @JsonProperty("environment_variables") private List<Map<String, String>> environmentVariables;
    @JsonProperty("tags") private List<Map<String, String>> tags;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public Map<String, Object> getArtifactSource() { return artifactSource; }
    public Map<String, Object> getIdentityConfiguration() { return identityConfiguration; }
    public Map<String, Object> getInvokeConfig() { return invokeConfig; }
    public Map<String, Object> getNetworkConfig() { return networkConfig; }
    public Map<String, Object> getObservability() { return observability; }
    public String getExecutionAgencyName() { return executionAgencyName; }
    public String getAgentGatewayId() { return agentGatewayId; }
    public Map<String, Object> getWorkloadIdentity() { return workloadIdentity; }
    public List<Map<String, String>> getEnvironmentVariables() { return environmentVariables; }
    public List<Map<String, String>> getTags() { return tags; }
}
