package com.huaweicloud.agentarts.sdk.mcpgateway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Request body for updating an MCP gateway.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateMcpGatewayRequest {

    @JsonProperty("description")
    private String description;

    public UpdateMcpGatewayRequest() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UpdateMcpGatewayRequest withDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateMcpGatewayRequest that = (UpdateMcpGatewayRequest) o;
        return Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description);
    }

    @Override
    public String toString() {
        return "UpdateMcpGatewayRequest{"
                + "description='" + description + '\''
                + '}';
    }
}
