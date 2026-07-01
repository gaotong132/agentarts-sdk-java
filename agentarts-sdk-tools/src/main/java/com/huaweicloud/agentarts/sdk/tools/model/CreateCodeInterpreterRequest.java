package com.huaweicloud.agentarts.sdk.tools.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Request body for creating a Code Interpreter.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateCodeInterpreterRequest {

    @JsonProperty("name")
    private String name;

    @JsonProperty("auth_type")
    private String authType;

    @JsonProperty("api_key_name")
    private String apiKeyName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("execution_agency_name")
    private String executionAgencyName;

    @JsonProperty("observability")
    private Map<String, Object> observability;

    @JsonProperty("network_config")
    private Map<String, Object> networkConfig;

    @JsonProperty("agent_gateway_id")
    private String agentGatewayId;

    @JsonProperty("tags")
    private List<Map<String, String>> tags;

    public CreateCodeInterpreterRequest withName(String name) { this.name = name; return this; }
    public CreateCodeInterpreterRequest withAuthType(String authType) { this.authType = authType; return this; }
    public CreateCodeInterpreterRequest withApiKeyName(String apiKeyName) { this.apiKeyName = apiKeyName; return this; }
    public CreateCodeInterpreterRequest withDescription(String description) { this.description = description; return this; }
    public CreateCodeInterpreterRequest withExecutionAgencyName(String executionAgencyName) { this.executionAgencyName = executionAgencyName; return this; }
    public CreateCodeInterpreterRequest withObservability(Map<String, Object> observability) { this.observability = observability; return this; }
    public CreateCodeInterpreterRequest withNetworkConfig(Map<String, Object> networkConfig) { this.networkConfig = networkConfig; return this; }
    public CreateCodeInterpreterRequest withAgentGatewayId(String agentGatewayId) { this.agentGatewayId = agentGatewayId; return this; }
    public CreateCodeInterpreterRequest withTags(List<Map<String, String>> tags) { this.tags = tags; return this; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    public String getApiKeyName() { return apiKeyName; }
    public void setApiKeyName(String apiKeyName) { this.apiKeyName = apiKeyName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getExecutionAgencyName() { return executionAgencyName; }
    public void setExecutionAgencyName(String executionAgencyName) { this.executionAgencyName = executionAgencyName; }
    public Map<String, Object> getObservability() { return observability; }
    public void setObservability(Map<String, Object> observability) { this.observability = observability; }
    public Map<String, Object> getNetworkConfig() { return networkConfig; }
    public void setNetworkConfig(Map<String, Object> networkConfig) { this.networkConfig = networkConfig; }
    public String getAgentGatewayId() { return agentGatewayId; }
    public void setAgentGatewayId(String agentGatewayId) { this.agentGatewayId = agentGatewayId; }
    public List<Map<String, String>> getTags() { return tags; }
    public void setTags(List<Map<String, String>> tags) { this.tags = tags; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CreateCodeInterpreterRequest that)) return false;
        return Objects.equals(name, that.name) && Objects.equals(authType, that.authType)
                && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, authType, description);
    }

    @Override
    public String toString() {
        return "CreateCodeInterpreterRequest{name='" + name + "', authType='" + authType + "'}";
    }
}
