package com.huaweicloud.agentarts.sdk.mcpgateway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Request body for creating an MCP gateway.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateMcpGatewayRequest {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("protocol_type")
    private String protocolType;

    @JsonProperty("authorizer_type")
    private String authorizerType;

    @JsonProperty("agency_name")
    private String agencyName;

    public CreateMcpGatewayRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CreateMcpGatewayRequest withName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CreateMcpGatewayRequest withDescription(String description) {
        this.description = description;
        return this;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }

    public CreateMcpGatewayRequest withProtocolType(String protocolType) {
        this.protocolType = protocolType;
        return this;
    }

    public String getAuthorizerType() {
        return authorizerType;
    }

    public void setAuthorizerType(String authorizerType) {
        this.authorizerType = authorizerType;
    }

    public CreateMcpGatewayRequest withAuthorizerType(String authorizerType) {
        this.authorizerType = authorizerType;
        return this;
    }

    public String getAgencyName() {
        return agencyName;
    }

    public void setAgencyName(String agencyName) {
        this.agencyName = agencyName;
    }

    public CreateMcpGatewayRequest withAgencyName(String agencyName) {
        this.agencyName = agencyName;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateMcpGatewayRequest that = (CreateMcpGatewayRequest) o;
        return Objects.equals(name, that.name)
                && Objects.equals(description, that.description)
                && Objects.equals(protocolType, that.protocolType)
                && Objects.equals(authorizerType, that.authorizerType)
                && Objects.equals(agencyName, that.agencyName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, protocolType, authorizerType, agencyName);
    }

    @Override
    public String toString() {
        return "CreateMcpGatewayRequest{"
                + "name='" + name + '\''
                + ", description='" + description + '\''
                + ", protocolType='" + protocolType + '\''
                + ", authorizerType='" + authorizerType + '\''
                + ", agencyName='" + agencyName + '\''
                + '}';
    }
}
