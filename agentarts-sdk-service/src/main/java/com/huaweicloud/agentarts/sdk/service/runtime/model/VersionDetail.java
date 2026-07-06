package com.huaweicloud.agentarts.sdk.service.runtime.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Per-version detail of a Runtime agent. An agent update creates a new version
 * whose mutable fields (description, artifact source, configs) live here.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VersionDetail {

    @JsonProperty("version") private String version;
    @JsonProperty("description") private String description;
    @JsonProperty("created_at") private String createdAt;
    @JsonProperty("updated_at") private String updatedAt;
    @JsonProperty("artifact_source") private Map<String, Object> artifactSource;
    @JsonProperty("network_config") private Map<String, Object> networkConfig;
    @JsonProperty("invoke_config") private Map<String, Object> invokeConfig;
    @JsonProperty("observability") private Map<String, Object> observability;

    public String getVersion() { return version; }
    public String getDescription() { return description; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public Map<String, Object> getArtifactSource() { return artifactSource; }
    public Map<String, Object> getNetworkConfig() { return networkConfig; }
    public Map<String, Object> getInvokeConfig() { return invokeConfig; }
    public Map<String, Object> getObservability() { return observability; }
}
