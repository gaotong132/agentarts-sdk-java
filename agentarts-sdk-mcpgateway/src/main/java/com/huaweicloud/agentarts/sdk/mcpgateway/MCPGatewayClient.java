package com.huaweicloud.agentarts.sdk.mcpgateway;

import com.huaweicloud.agentarts.sdk.core.Constants;
import com.huaweicloud.agentarts.sdk.core.SignMode;
import com.huaweicloud.agentarts.sdk.core.util.JsonUtils;
import com.huaweicloud.agentarts.sdk.mcpgateway.model.CreateMcpGatewayRequest;
import com.huaweicloud.agentarts.sdk.mcpgateway.model.CreateMcpGatewayTargetRequest;
import com.huaweicloud.agentarts.sdk.mcpgateway.model.UpdateMcpGatewayRequest;
import com.huaweicloud.agentarts.sdk.mcpgateway.model.UpdateMcpGatewayTargetRequest;
import com.huaweicloud.agentarts.sdk.service.http.BaseHttpClient;
import com.huaweicloud.agentarts.sdk.service.http.RequestConfig;
import com.huaweicloud.agentarts.sdk.service.http.RequestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP Gateway client for managing gateways and targets via AK/SK signed requests.
 * Always uses AK/SK signing (SDK-HMAC-SHA256). Base URL is control plane + /v1/core.
 *
 * <p>When creating a gateway with {@code authorizerType="iam"} and no explicit
 * {@code agencyName}, an IAM agency named {@value #DEFAULT_AGENCY_NAME} is
 * automatically created (or reused if it already exists).</p>
 *
 * <h3>Gateway management:</h3>
 * create/update/delete/get/list_mcp_gateway
 *
 * <h3>Target management:</h3>
 * create/update/delete/get/list_mcp_gateway_target
 */
public class MCPGatewayClient implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(MCPGatewayClient.class);

    /** Default IAM agency name for MCP Gateway. */
    public static final String DEFAULT_AGENCY_NAME = "AgentArtsCoreGateway";

    /**
     * Build the IAM trust policy JSON string, matching Python json.dumps() format exactly.
     */
    private static String buildTrustPolicy() {
        // Match Python json.dumps() output format exactly (spaces after : and ,)
        return "{\"Version\": \"5.0\", \"Statement\": [{\"Action\": ["
                + "\"csms:secret:getVersion\", "
                + "\"agentIdentity::getResourceApiKey\", "
                + "\"agentIdentity::getResourceOauth2Token\", "
                + "\"agentIdentity::getResourceStsToken\""
                + "], \"Effect\": \"Allow\", "
                + "\"Principal\": {\"Service\": [\"service.WorkloadSandboxMetadata\"]}}]}";
    }

    private final BaseHttpClient httpClient;
    private final boolean verifySsl;

    public MCPGatewayClient(boolean verifySsl) {
        this.verifySsl = verifySsl;
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
     *
     * <p>When {@code authorizerType} is "iam" and {@code agencyName} is null,
     * an IAM agency named {@value #DEFAULT_AGENCY_NAME} is automatically created
     * (or reused if it already exists), matching the Python SDK behavior.</p>
     */
    public RequestResult createMcpGateway(String name, String description,
                                           String protocolType, String authorizerType,
                                           String agencyName) {
        String protocol = protocolType != null ? protocolType : "mcp";
        String authorizer = authorizerType != null ? authorizerType : "iam";

        // Auto-create IAM agency when using IAM auth and no agency name provided
        String resolvedAgencyName = agencyName;
        if ("iam".equals(authorizer) && agencyName == null) {
            resolvedAgencyName = ensureIamAgency();
        }

        CreateMcpGatewayRequest req = new CreateMcpGatewayRequest()
                .withName(name)
                .withDescription(description)
                .withProtocolType(protocol)
                .withAuthorizerType(authorizer)
                .withAgencyName(resolvedAgencyName)
                .withLogDeliveryConfiguration(Map.of("enabled", false))
                .withOutboundNetworkConfiguration(Map.of("network_mode", "public"));
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

    // ========================
    // Internal helpers
    // ========================

    /**
     * Ensure the default IAM agency exists, creating it if necessary.
     * Returns the agency name. Errors during creation are logged and
     * the default name is still returned (the server may already have it).
     */
    private String ensureIamAgency() {
        try {
            // Use BasicCredentials directly for IAM v5 agency API
            // (IAMServiceClient uses GlobalCredentials which doesn't work for v5)
            com.huaweicloud.sdk.core.auth.BasicCredentials credentials =
                    new com.huaweicloud.sdk.core.auth.BasicCredentials()
                            .withAk(Constants.getAk())
                            .withSk(Constants.getSk());
            String securityToken = Constants.getSecurityToken();
            if (JsonUtils.isNotBlank(securityToken)) {
                credentials.withSecurityToken(securityToken);
            }

            com.huaweicloud.sdk.core.http.HttpConfig httpConfig =
                    com.huaweicloud.sdk.core.http.HttpConfig.getDefaultHttpConfig();
            httpConfig.setIgnoreSSLVerification(!verifySsl);

            String endpoint = Constants.getIamEndpoint(Constants.getRegion());

            com.huaweicloud.sdk.iam.v5.IamClient iamClient =
                    new com.huaweicloud.sdk.core.ClientBuilder<>(com.huaweicloud.sdk.iam.v5.IamClient::new)
                            .withCredential(credentials)
                            .withHttpConfig(httpConfig)
                            .withEndpoint(endpoint)
                            .build();

            com.huaweicloud.sdk.iam.v5.model.CreateAgencyReqBody body =
                    new com.huaweicloud.sdk.iam.v5.model.CreateAgencyReqBody()
                            .withAgencyName(DEFAULT_AGENCY_NAME)
                            .withTrustPolicy(buildTrustPolicy());

            com.huaweicloud.sdk.iam.v5.model.CreateAgencyV5Request request =
                    new com.huaweicloud.sdk.iam.v5.model.CreateAgencyV5Request().withBody(body);

            iamClient.createAgencyV5(request);
            LOG.info("Created IAM agency: {}", DEFAULT_AGENCY_NAME);
        } catch (Exception e) {
            // 409 Conflict = agency already exists, which is fine
            String msg = e.getMessage();
            if (msg != null && msg.contains("409")) {
                LOG.debug("IAM agency {} already exists", DEFAULT_AGENCY_NAME);
            } else {
                LOG.warn("Failed to create IAM agency {}: {}", DEFAULT_AGENCY_NAME, msg);
            }
        }
        return DEFAULT_AGENCY_NAME;
    }
}
