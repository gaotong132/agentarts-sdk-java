package com.huaweicloud.agentarts.toolkit.operations;

import com.huaweicloud.agentarts.sdk.core.Constants;
import com.huaweicloud.agentarts.sdk.core.config.AgentArtsConfig;
import com.huaweicloud.agentarts.sdk.core.config.AgentArtsConfigList;
import com.huaweicloud.agentarts.sdk.service.runtime.RuntimeClient;

/**
 * Destroy operation: delete a deployed agent from Huawei Cloud.
 *
 * <p>Operation: destroy a deployed agent from Huawei Cloud.
 * Resolves the agent name/region from {@code .agentarts_config.yaml} when not
 * provided, then deletes the agent via the control plane (find-by-name → delete-by-id).</p>
 */
public class DestroyOperation {

    /**
     * Destroy (delete) an agent from Huawei Cloud.
     *
     * @param agentName agent name to destroy (uses config default when null)
     * @param region    Huawei Cloud region (uses config/env default when null)
     * @param skipSsl   skip SSL verification
     * @return {@code true} if the agent was destroyed (or already absent)
     */
    public static boolean destroyAgent(String agentName, String region, boolean skipSsl) throws Exception {
        String name = agentName;
        String actualRegion = region;

        // Resolve name/region from config when not explicitly provided.
        if (name == null || name.isBlank()) {
            AgentArtsConfigList config = ConfigOperation.loadConfig();
            String key = config.getDefaultAgent();
            if (key != null) {
                AgentArtsConfig agent = config.getAgent(key);
                if (agent != null && agent.getBase() != null) {
                    name = agent.getBase().getName();
                    if (actualRegion == null || actualRegion.isBlank()) {
                        actualRegion = agent.getBase().getRegion();
                    }
                }
            }
            if (name == null || name.isBlank()) {
                throw new IllegalStateException("No agent specified and no default agent configured.");
            }
        }
        if (actualRegion == null || actualRegion.isBlank()) {
            actualRegion = Constants.getRegion();
        }

        System.out.println("Destroying agent '" + name + "' in region " + actualRegion + "...");
        try (RuntimeClient client = new RuntimeClient(actualRegion, !skipSsl)) {
            boolean deleted = client.deleteAgentByName(name);
            if (deleted) {
                System.out.println("Agent '" + name + "' destroyed successfully.");
                return true;
            }
            // Already absent counts as success for teardown/idempotency.
            System.out.println("Agent '" + name + "' not found (already destroyed).");
            return true;
        } catch (Exception e) {
            System.err.println("Failed to destroy agent '" + name + "': " + e.getMessage());
            throw e;
        }
    }
}
