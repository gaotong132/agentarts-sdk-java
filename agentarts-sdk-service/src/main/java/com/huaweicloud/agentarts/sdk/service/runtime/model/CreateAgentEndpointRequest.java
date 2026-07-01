package com.huaweicloud.agentarts.sdk.service.runtime.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

/**
 * Request body for creating an agent endpoint.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateAgentEndpointRequest {

    @JsonProperty("endpoint_name")
    private String endpointName;

    @JsonProperty("name")
    private String name;

    @JsonProperty("agent_id")
    private String agentId;

    @JsonProperty("endpoint_type")
    private String endpointType;

    @JsonProperty("target_version_name")
    private String targetVersionName;

    @JsonProperty("config")
    private Map<String, Object> config;

    public CreateAgentEndpointRequest withEndpointName(String endpointName) {
        this.endpointName = endpointName;
        return this;
    }

    public CreateAgentEndpointRequest withName(String name) {
        this.name = name;
        return this;
    }

    public CreateAgentEndpointRequest withAgentId(String agentId) {
        this.agentId = agentId;
        return this;
    }

    public CreateAgentEndpointRequest withEndpointType(String endpointType) {
        this.endpointType = endpointType;
        return this;
    }

    public CreateAgentEndpointRequest withTargetVersionName(String targetVersionName) {
        this.targetVersionName = targetVersionName;
        return this;
    }

    public CreateAgentEndpointRequest withConfig(Map<String, Object> config) {
        this.config = config;
        return this;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getEndpointType() {
        return endpointType;
    }

    public void setEndpointType(String endpointType) {
        this.endpointType = endpointType;
    }

    public String getTargetVersionName() {
        return targetVersionName;
    }

    public void setTargetVersionName(String targetVersionName) {
        this.targetVersionName = targetVersionName;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateAgentEndpointRequest that = (CreateAgentEndpointRequest) o;
        return Objects.equals(endpointName, that.endpointName)
                && Objects.equals(name, that.name)
                && Objects.equals(agentId, that.agentId)
                && Objects.equals(endpointType, that.endpointType)
                && Objects.equals(targetVersionName, that.targetVersionName)
                && Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpointName, name, agentId, endpointType, targetVersionName, config);
    }

    @Override
    public String toString() {
        return "CreateAgentEndpointRequest{"
                + "endpointName='" + endpointName + "'"
                + ", name='" + name + "'"
                + ", agentId='" + agentId + "'"
                + ", endpointType='" + endpointType + "'"
                + ", targetVersionName='" + targetVersionName + "'"
                + ", config=" + config
                + "}";
    }
}
