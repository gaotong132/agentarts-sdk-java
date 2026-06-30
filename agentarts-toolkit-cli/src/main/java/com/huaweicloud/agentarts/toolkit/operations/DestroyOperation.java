package com.huaweicloud.agentarts.toolkit.operations;

/**
 * Destroy operation: delete agent from Huawei Cloud.
 * Mirrors Python operations/runtime/destroy.py.
 */
public class DestroyOperation {
    /**
     * Destroy (delete) an agent from Huawei Cloud.
     *
     * @param agentName agent name to destroy
     * @param region    Huawei Cloud region
     * @param skipSsl   skip SSL verification
     */
    public static void destroyAgent(String agentName, String region, boolean skipSsl) throws Exception {
        System.out.println("Destroying agent '" + agentName + "' in region " + region + "...");
        // TODO: RuntimeServiceClient.deleteAgent with V11 signing
    }
}
