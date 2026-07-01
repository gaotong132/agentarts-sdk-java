package com.huaweicloud.agentarts.sdk.mcpgateway;

import com.huaweicloud.agentarts.sdk.core.Constants;
import com.huaweicloud.agentarts.sdk.core.SignMode;
import com.huaweicloud.agentarts.sdk.service.http.BaseHttpClient;
import com.huaweicloud.agentarts.sdk.service.http.RequestConfig;
import com.huaweicloud.agentarts.sdk.service.http.RequestResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP Gateway client for managing MCP gateways and targets.
 *
 * <p>MCP Gateway client for managing gateways and targets via AK/SK signed requests.
 * Always uses AK/SK signing (SDK-HMAC-SHA256). Base URL is control plane + /v1/core.</p>
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
     * Create an MCP gateway.
     * Create an MCP gateway with all parameters.
     */
    public RequestResult createMcpGateway(String name, String description,
                                           String protocolType, String authorizerType,
                                           String agencyName) {
        Map<String, Object> body = new HashMap<>();
        if (name != null) body.put("name", name);
        if (description != null) body.put("description", description);
        body.put("protocol_type", protocolType != null ? protocolType : "mcp");
        body.put("authorizer_type", authorizerType != null ? authorizerType : "iam");
        if (agencyName != null) body.put("agency_name", agencyName);
        return httpClient.post("/gateways", null, body).block();
    }

    public RequestResult createMcpGateway(String name, String description) {
        return createMcpGateway(name, description, "mcp", "iam", null);
    }

    public RequestResult updateMcpGateway(String gatewayId, String description) {
        Map<String, Object> body = new HashMap<>();
        if (description != null) body.put("description", description);
        return httpClient.put("/gateways/" + gatewayId, null, body).block();
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
        Map<String, Object> body = new HashMap<>();
        if (name != null) body.put("name", name);
        if (description != null) body.put("description", description);
        if (targetConfiguration != null) body.put("target_configuration", targetConfiguration);
        if (credentialProviderConfiguration != null) {
            body.put("credential_provider_configuration", credentialProviderConfiguration);
        }
        return httpClient.post("/gateways/" + gatewayId + "/targets", null, body).block();
    }

    public RequestResult createMcpGatewayTarget(String gatewayId, String name, String description) {
        return createMcpGatewayTarget(gatewayId, name, description, null, null);
    }

    public RequestResult updateMcpGatewayTarget(String gatewayId, String targetId,
                                                   String name, String description,
                                                   Map<String, Object> targetConfiguration,
                                                   Map<String, Object> credentialProviderConfiguration) {
        Map<String, Object> body = new HashMap<>();
        if (name != null) body.put("name", name);
        if (description != null) body.put("description", description);
        if (targetConfiguration != null) body.put("target_configuration", targetConfiguration);
        if (credentialProviderConfiguration != null) {
            body.put("credential_provider_configuration", credentialProviderConfiguration);
        }
        return httpClient.put("/gateways/" + gatewayId + "/targets/" + targetId, null, body).block();
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
