package com.huaweicloud.agentarts.sdk.mcpgateway;

import com.huaweicloud.agentarts.sdk.core.Constants;
import com.huaweicloud.agentarts.sdk.core.SignMode;
import com.huaweicloud.agentarts.sdk.mcpgateway.model.CreateMcpGatewayRequest;
import com.huaweicloud.agentarts.sdk.mcpgateway.model.CreateMcpGatewayTargetRequest;
import com.huaweicloud.agentarts.sdk.mcpgateway.model.UpdateMcpGatewayRequest;
import com.huaweicloud.agentarts.sdk.mcpgateway.model.UpdateMcpGatewayTargetRequest;
import com.huaweicloud.agentarts.sdk.service.http.BaseHttpClient;
import com.huaweicloud.agentarts.sdk.service.http.RequestConfig;
import com.huaweicloud.agentarts.sdk.service.http.RequestResult;

import java.util.Map;

/**
 * MCP Gateway client for managing gateways and targets via AK/SK signed requests.
 * Always uses AK/SK signing (SDK-HMAC-SHA256). Base URL is control plane + /v1/core.
 *
 * <h3>Gateway management:</h3>
 * create/update/delete/get/list_mcp_gateway
 *
 * <h3>Target management:</h3>
 * create/update/delete/get/list_mcp_gateway_target
 */
public class MCPGatewayClient implements AutoCloseable {

    private final BaseHttpClient httpClient;

    public MCPGatewayClient(boolean verifySsl) {
        String endpoint = Constants.getControlPlaneEndpoint() + "/v1/core";
        RequestConfig config = RequestConfig.builder()
                .baseUrl(endpoint)
                .verifySsl(verifySsl)
                .build();
        this.httpClient = new BaseHttpClient(config, true, SignMode.SDK_HMAC_SHA256, Constants.getRegion());
    }

    public MCPGatewayClient() {
        this(true);
    }

    // ========================
    // Gateway CRUD
    // ========================

    /**
     * Create an MCP gateway with all parameters.
     */
    public RequestResult createMcpGateway(String name, String description,
                                           String protocolType, String authorizerType,
                                           String agencyName) {
        String protocol = protocolType != null ? protocolType : "mcp";
        String authorizer = authorizerType != null ? authorizerType : "iam";
        CreateMcpGatewayRequest req = new CreateMcpGatewayRequest()
                .withName(name)
                .withDescription(description)
                .withProtocolType(protocol)
                .withAuthorizerType(authorizer)
                .withAgencyName(agencyName);
        return httpClient.post("/gateways", null, req).block();
    }

    public RequestResult createMcpGateway(String name, String description) {
        return createMcpGateway(name, description, "mcp", "iam", null);
    }

    public RequestResult updateMcpGateway(String gatewayId, String description) {
        UpdateMcpGatewayRequest req = new UpdateMcpGatewayRequest()
                .withDescription(description);
        return httpClient.put("/gateways/" + gatewayId, null, req).block();
    }

    public RequestResult deleteMcpGateway(String gatewayId) {
        return httpClient.delete("/gateways/" + gatewayId).block();
    }

    public RequestResult getMcpGateway(String gatewayId) {
        return httpClient.get("/gateways/" + gatewayId).block();
    }

    public RequestResult listMcpGateways(String name, Integer limit, Integer offset) {
        StringBuilder url = new StringBuilder("/gateways?");
        if (name != null) url.append("name=").append(name).append("&");
        if (limit != null) url.append("limit=").append(limit).append("&");
        if (offset != null) url.append("offset=").append(offset).append("&");
        return httpClient.get(url.toString()).block();
    }

    public RequestResult listMcpGateways() {
        return listMcpGateways(null, null, null);
    }

    // ========================
    // Target CRUD
    // ========================

    public RequestResult createMcpGatewayTarget(String gatewayId, String name,
                                                  String description,
                                                  Map<String, Object> targetConfiguration,
                                                  Map<String, Object> credentialProviderConfiguration) {
        CreateMcpGatewayTargetRequest req = new CreateMcpGatewayTargetRequest()
                .withName(name)
                .withDescription(description)
                .withTargetConfiguration(targetConfiguration)
                .withCredentialProviderConfiguration(credentialProviderConfiguration);
        return httpClient.post("/gateways/" + gatewayId + "/targets", null, req).block();
    }

    public RequestResult createMcpGatewayTarget(String gatewayId, String name, String description) {
        return createMcpGatewayTarget(gatewayId, name, description, null, null);
    }

    public RequestResult updateMcpGatewayTarget(String gatewayId, String targetId,
                                                   String name, String description,
                                                   Map<String, Object> targetConfiguration,
                                                   Map<String, Object> credentialProviderConfiguration) {
        UpdateMcpGatewayTargetRequest req = new UpdateMcpGatewayTargetRequest()
                .withName(name)
                .withDescription(description)
                .withTargetConfiguration(targetConfiguration)
                .withCredentialProviderConfiguration(credentialProviderConfiguration);
        return httpClient.put("/gateways/" + gatewayId + "/targets/" + targetId, null, req).block();
    }

    public RequestResult deleteMcpGatewayTarget(String gatewayId, String targetId) {
        return httpClient.delete("/gateways/" + gatewayId + "/targets/" + targetId).block();
    }

    public RequestResult getMcpGatewayTarget(String gatewayId, String targetId) {
        return httpClient.get("/gateways/" + gatewayId + "/targets/" + targetId).block();
    }

    public RequestResult listMcpGatewayTargets(String gatewayId, Integer limit, Integer offset) {
        StringBuilder url = new StringBuilder("/gateways/" + gatewayId + "/targets?");
        if (limit != null) url.append("limit=").append(limit).append("&");
        if (offset != null) url.append("offset=").append(offset).append("&");
        return httpClient.get(url.toString()).block();
    }

    public RequestResult listMcpGatewayTargets(String gatewayId) {
        return listMcpGatewayTargets(gatewayId, null, null);
    }

    @Override
    public void close() {
        httpClient.close();
    }
}
