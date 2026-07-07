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

/**
 * Resolves a configured {@link RuntimeClient} for the data plane from the
 * project's {@code .agentarts_config.yaml}: agent id, auth type, and the
 * agent's {@code access_endpoint} (discovered via the control plane).
 *
 * <p>Mirrors the reference CLI's invoke/runtime resolution: prefer the
 * {@code AGENTARTS_RUNTIME_DATA_ENDPOINT} env var; otherwise look up the agent
 * by id (from config) or name on the control plane and read
 * {@code version_detail.invoke_config.access_endpoint}. IAM agents use V11
 * signing; others require a bearer token and use SDK-HMAC-SHA256.</p>
 */
public final class RuntimeResolver {

    private RuntimeResolver() {}

    /**
     * Resolve a data-plane client for the given agent.
     *
     * @param agentName    agent name (uses config default when null)
     * @param regionHint   region override (uses config/env default when null)
     * @param verifySsl    whether to verify SSL
     * @param bearerToken  bearer token (required for non-IAM agents)
     * @return a configured, open {@link RuntimeClient}
     */
    public static RuntimeClient resolve(String agentName, String regionHint,
                                         boolean verifySsl, String bearerToken) {
        AgentArtsConfigList config = loadConfigIfExists();
        String key = (agentName != null && !agentName.isBlank()) ? agentName : config.getDefaultAgent();
        AgentArtsConfig agentCfg = (key != null) ? config.getAgent(key) : null;

        String region = resolveRegion(regionHint, agentCfg);
        String agentId = (agentCfg != null && agentCfg.getRuntime() != null)
                ? agentCfg.getRuntime().getAgentId() : null;
        String authType = resolveAuthType(agentCfg);

        SignMode signMode;
        if (authType != null && "IAM".equalsIgnoreCase(authType)) {
            signMode = SignMode.V11_HMAC_SHA256;
        } else if (JsonUtils.isNotBlank(bearerToken)) {
            signMode = SignMode.SDK_HMAC_SHA256;
        } else {
            // Fall back to V11 (best effort) — the backend will reject if the agent
            // actually requires a bearer token. This keeps IAM-only agents working
            // without forcing the caller to pass an empty bearer token.
            signMode = SignMode.V11_HMAC_SHA256;
        }

        // Data endpoint: env override first, else discover from the control plane.
        String dataEndpoint = Constants.getRuntimeDataPlaneEndpoint();
        if (!JsonUtils.isNotBlank(dataEndpoint)) {
            dataEndpoint = discoverAccessEndpoint(agentName, key, agentId, region, verifySsl);
        }

        RuntimeClient client = new RuntimeClient(region, verifySsl, signMode);
        if (JsonUtils.isNotBlank(dataEndpoint)) {
            client.setDataPlaneEndpoint(dataEndpoint);
        }
        return client;
    }

    private static AgentArtsConfigList loadConfigIfExists() {
        // Only load when a project config is present, so that running a runtime
        // subcommand outside a project dir does not create an empty config file.
        // Resolve against the live `user.dir` (see DeployOperation/ConfigOperation
        // for rationale — relative paths ignore runtime `user.dir` updates).
        File cfg = new File(System.getProperty("user.dir", "."), ".agentarts_config.yaml");
        if (!cfg.exists()) {
            return new AgentArtsConfigList();
        }
        return ConfigOperation.loadConfig();
    }

    private static String resolveRegion(String regionHint, AgentArtsConfig agentCfg) {
        if (JsonUtils.isNotBlank(regionHint)) return regionHint;
        if (agentCfg != null && agentCfg.getBase() != null
                && JsonUtils.isNotBlank(agentCfg.getBase().getRegion())) {
            return agentCfg.getBase().getRegion();
        }
        return Constants.getRegion();
    }

    @SuppressWarnings("unchecked")
    private static String resolveAuthType(AgentArtsConfig agentCfg) {
        if (agentCfg == null || agentCfg.getRuntime() == null) return null;
        Map<String, Object> identity = agentCfg.getRuntime().getIdentityConfiguration();
        if (identity == null) return null;
        Object t = identity.get("authorizer_type");
        return t != null ? String.valueOf(t) : null;
    }

    private static String discoverAccessEndpoint(String agentName, String configKey,
                                                  String agentId, String region, boolean verifySsl) {
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
            VersionDetail vd = info.getVersionDetail();
            if (vd == null || vd.getInvokeConfig() == null) return null;
            Object ae = vd.getInvokeConfig().get("access_endpoint");
            return ae != null ? String.valueOf(ae) : null;
        } catch (Exception e) {
            System.err.println("[runtime] Could not resolve access_endpoint from control plane: "
                    + e.getMessage());
            return null;
        }
    }
}
