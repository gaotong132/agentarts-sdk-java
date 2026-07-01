package com.huaweicloud.agentarts.sdk.service.runtime.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

/**
 * Request body for updating an agent endpoint.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateAgentEndpointRequest {

    @JsonProperty("config")
    private Map<String, Object> config;

    public UpdateAgentEndpointRequest withConfig(Map<String, Object> config) {
        this.config = config;
        return this;
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
        UpdateAgentEndpointRequest that = (UpdateAgentEndpointRequest) o;
        return Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(config);
    }

    @Override
    public String toString() {
        return "UpdateAgentEndpointRequest{config=" + config + "}";
    }
}
