package com.huaweicloud.agentarts.toolkit.operations;

/**
 * Deploy operation: build → SWR → runtime create.
 * Mirrors Python operations/runtime/deploy.py.
 * Flow: docker build → SWR createNamespace/createRepo → createAuthorizationToken → runtime create (V11 signed).
 */
public class DeployOperation {
    /**
     * Deploy an agent to Huawei Cloud or run locally.
     *
     * @param agentName   agent name to deploy
     * @param mode        deployment mode: "cloud" or "local"
     * @param imageTag    Docker image tag (default "latest")
     * @param localPort   port for local mode (nullable)
     * @param swrOrg      SWR organization (nullable)
     * @param swrRepo     SWR repository (nullable)
     * @param description agent description (nullable)
     * @param skipBuild   skip Docker build step
     * @param skipSsl     skip SSL verification
     */
    public static void deployProject(String agentName, String mode, String imageTag,
                                      Integer localPort, String swrOrg, String swrRepo,
                                      String description, boolean skipBuild, boolean skipSsl) throws Exception {
        if ("local".equals(mode)) {
            System.out.println("Building and running locally...");
            // TODO: docker build + docker run
            return;
        }
        // Cloud mode
        if (!skipBuild) {
            System.out.println("Building Docker image: " + agentName + ":" + imageTag);
            // TODO: docker-java build
            System.out.println("Pushing to SWR: " + swrOrg + "/" + swrRepo);
            // TODO: SWR createNamespace + createRepo + createAuthorizationToken + push
        }
        System.out.println("Creating AgentArts runtime for '" + agentName + "'...");
        // TODO: RuntimeServiceClient.createAgent with V11 signing
    }
}
