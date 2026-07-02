package com.huaweicloud.agentarts.sdk.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * API key information returned by createApiKey.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiKeyInfo {
    @JsonProperty("id") private String id;
    @JsonProperty("api_key") private String apiKey;
    @JsonProperty("name") private String name;
    @JsonProperty("created_at") private String createdAt;

    public String getId() { return id; }
    public String getApiKey() { return apiKey; }
    public String getName() { return name; }
    public String getCreatedAt() { return createdAt; }
}
