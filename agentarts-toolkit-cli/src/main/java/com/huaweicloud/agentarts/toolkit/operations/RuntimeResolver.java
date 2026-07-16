package com.huaweicloud.agentarts.toolkit.operations;

import com.huaweicloud.agentarts.sdk.core.Constants;
import com.huaweicloud.agentarts.sdk.core.SignMode;
import com.huaweicloud.agentarts.sdk.core.config.AgentArtsConfig;
import com.huaweicloud.agentarts.sdk.core.config.AgentArtsConfigList;
import com.huaweicloud.agentarts.sdk.core.util.JsonUtils;
import com.huaweicloud.agentarts.sdk.service.runtime.RuntimeClient;
import com.huaweicloud.agentarts.sdk.service.runtime.model.AgentInfo;
import com.huaweicloud.agentarts.sdk.service.runtime.model.VersionDetail;

import java.io.File;
import java.util.Map;

/** Resolves a production-ready runtime data-plane client from CLI configuration. */
public final class RuntimeResolver {

    private RuntimeResolver() {}

    /**
     * Resolve the authentication mode and data-plane endpoint for an agent.
     * Unknown/non-IAM authentication fails closed unless a bearer token is supplied.
     */
    public static RuntimeClient resolve(String agentName, String regionHint,
                                         boolean verifySsl, String bearerToken) {
        return resolve(agentName, regionHint, verifySsl, bearerToken, null);
    }

    /** Resolve a runtime client, preferring an explicitly supplied data endpoint. */
    public static RuntimeClient resolve(String agentName, String regionHint,
                                         boolean verifySsl, String bearerToken,
                                         String dataEndpointHint) {
        AgentArtsConfigList config = loadConfigIfExists();
        String key = JsonUtils.isNotBlank(agentName) ? agentName : config.getDefaultAgent();
        AgentArtsConfig agentConfig = key != null ? config.getAgent(key) : null;

        String region = resolveRegion(regionHint, agentConfig);
        String agentId = agentConfig != null && agentConfig.getRuntime() != null
                ? agentConfig.getRuntime().getAgentId() : null;
        String authType = resolveAuthType(agentConfig);

        SignMode signMode;
        if (authType != null && "IAM".equalsIgnoreCase(authType)) {
            signMode = SignMode.V11_HMAC_SHA256;
        } else if (JsonUtils.isNotBlank(bearerToken)) {
            signMode = SignMode.SDK_HMAC_SHA256;
        } else {
            throw new IllegalArgumentException(
                    "Bearer token is required for non-IAM or unknown authentication");
        }

        String dataEndpoint = JsonUtils.isNotBlank(dataEndpointHint)
                ? dataEndpointHint : Constants.getRuntimeDataPlaneEndpoint();
        if (!JsonUtils.isNotBlank(dataEndpoint)) {
            dataEndpoint = discoverAccessEndpoint(agentName, key, agentId, region, verifySsl);
        }
        if (!JsonUtils.isNotBlank(dataEndpoint)) {
            throw new IllegalStateException(
                    "No runtime data endpoint configured and access endpoint discovery failed");
        }

        RuntimeClient client = new RuntimeClient(region, verifySsl, signMode);
        client.setDataPlaneEndpoint(dataEndpoint);
        return client;
    }

    /** Resolve an explicit agent name or the configured default without creating a config file. */
    public static String resolveAgentName(String agentName) {
        if (JsonUtils.isNotBlank(agentName)) return agentName;
        return loadConfigIfExists().getDefaultAgent();
    }

    private static AgentArtsConfigList loadConfigIfExists() {
        File file = new File(System.getProperty("user.dir", "."), ".agentarts_config.yaml");
        return file.exists() ? ConfigOperation.loadConfig() : new AgentArtsConfigList();
    }

    private static String resolveRegion(String regionHint, AgentArtsConfig agentConfig) {
        if (JsonUtils.isNotBlank(regionHint)) return regionHint;
        if (agentConfig != null && agentConfig.getBase() != null
                && JsonUtils.isNotBlank(agentConfig.getBase().getRegion())) {
            return agentConfig.getBase().getRegion();
        }
        return Constants.getRegion();
    }

    private static String resolveAuthType(AgentArtsConfig agentConfig) {
        if (agentConfig == null || agentConfig.getRuntime() == null) return null;
        Map<String, Object> identity = agentConfig.getRuntime().getIdentityConfiguration();
        if (identity == null) return null;
        Object value = identity.get("authorizer_type");
        return value != null ? String.valueOf(value) : null;
    }

    private static String discoverAccessEndpoint(String agentName, String configKey,
                                                  String agentId, String region,
                                                  boolean verifySsl) {
        try (RuntimeClient control = new RuntimeClient(region, verifySsl)) {
            AgentInfo info = null;
            if (JsonUtils.isNotBlank(agentId)) {
                info = control.findAgentById(agentId);
            }
            if (info == null && JsonUtils.isNotBlank(agentName)) {
                info = control.findAgentByName(agentName);
            }
            if (info == null && JsonUtils.isNotBlank(configKey)) {
                info = control.findAgentByName(configKey);
            }
            if (info == null) return null;
            VersionDetail detail = info.getVersionDetail();
            if (detail == null || detail.getInvokeConfig() == null) return null;
            Object endpoint = detail.getInvokeConfig().get("access_endpoint");
            return endpoint != null ? String.valueOf(endpoint) : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
